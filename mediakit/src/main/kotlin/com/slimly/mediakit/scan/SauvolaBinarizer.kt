package com.slimly.mediakit.scan

import kotlin.math.max
import kotlin.math.sqrt

/**
 * Двухцветная страница из полутоновой: решить про каждый пиксель, чернила
 * это или бумага, и упаковать ответ по биту на пиксель.
 */
internal fun interface PageBinarizing {
    fun bitmap(page: GrayscaleBuffer): ScannedPage.Bilevel
}

/**
 * Порог Саувола — локальный, со скидкой на разброс яркости вокруг пикселя.
 *
 * Один порог на всю страницу работает, только пока лист освещён ровно. Стоит
 * тени от руки лечь на угол — и тот же порог там съедает текст, а на
 * пересвеченном крае оставляет серый мусор. Локальный порог считается
 * по окну вокруг пикселя и такой страницы не боится.
 *
 * Простого «темнее среднего по окну» при этом мало: на пустом поле среднее
 * равно самой бумаге, и половина шума уходит в чернила. Саувола добавляет
 * поправку на разброс — на ровном поле разброса нет, порог проваливается
 * ниже бумаги, и поле остаётся чистым:
 *
 *     T = m · (1 + k · (s / R − 1))
 *
 * где `m` и `s` — среднее и разброс в окне, `R` — половина шкалы яркости.
 */
internal class SauvolaBinarizer : PageBinarizing {
    override fun bitmap(page: GrayscaleBuffer): ScannedPage.Bilevel {
        if (page.width <= 0 || page.height <= 0) return packed(page)

        val window = max(MINIMUM_WINDOW, max(page.width, page.height) / SIDE_TO_WINDOW)
        val mask = SauvolaPass(page, radius = window / 2).run()

        return packed(GrayscaleBuffer(page.width, page.height, mask))
    }

    /**
     * Порог уже наложен, и в буфере остались только два значения — здесь
     * выбрасываются семь лишних бит из каждого байта. Страница ужимается
     * восьмикратно ещё до того, как PDF дожмёт её Flate; ровно поэтому
     * двухцветному режиму можно позволить разрешение, которого не выдержал
     * бы JPEG.
     */
    private fun packed(mask: GrayscaleBuffer): ScannedPage.Bilevel {
        val width = mask.width
        val rowBytes = (width + BITS_IN_BYTE - 1) / BITS_IN_BYTE
        val bits = ByteArray(rowBytes * mask.height)

        for (y in 0 until mask.height) {
            val row = y * rowBytes
            val line = y * width
            for (x in 0 until width) {
                if (mask.samples[line + x].toInt() and BYTE_MASK >= WHITE_FROM) {
                    val byte = row + x / BITS_IN_BYTE
                    bits[byte] = (bits[byte].toInt() or (HIGH_BIT shr (x % BITS_IN_BYTE))).toByte()
                }
            }
        }

        return ScannedPage.Bilevel(width = width, height = mask.height, rowBytes = rowBytes, bits = bits)
    }

    private companion object {
        /**
         * Сторона окна как доля длинной стороны страницы. На четырёхстах dpi
         * это около сорока пикселей — примерно высота строчной буквы, то есть
         * окно всегда видит и букву, и бумагу вокруг неё.
         */
        const val SIDE_TO_WINDOW = 100

        /**
         * Окно меньше этого бессмысленно: внутри одной жирной буквы среднее
         * равно самой букве, и она вычитается сама из себя.
         */
        const val MINIMUM_WINDOW = 15

        const val WHITE_FROM = 128
        const val BITS_IN_BYTE = 8
        const val BYTE_MASK = 0xFF

        /** Левый пиксель строки — старший бит байта. */
        const val HIGH_BIT = 0x80
    }
}

/**
 * Один проход порога по странице.
 *
 * Состояние живёт ровно одну страницу, поэтому это отдельный объект внутри
 * метода, а не поля воркера. Окно едет по строкам и по столбцам, и суммы
 * в нём не пересчитываются: строка входит в окно, строка выходит — работы
 * на пиксель ровно столько же, сколько при одном общем пороге.
 */
private class SauvolaPass(page: GrayscaleBuffer, private val radius: Int) {
    private val samples = page.samples
    private val width = page.width
    private val height = page.height

    /** Суммы и суммы квадратов по столбцам в текущем вертикальном окне. */
    private val columnSum = IntArray(page.width)
    private val columnSquares = IntArray(page.width)
    private var rowsInWindow = 0

    fun run(): ByteArray {
        val mask = ByteArray(width * height)
        for (y in 0 until height) {
            slide(y)
            threshold(y, mask)
        }
        return mask
    }

    /** Сдвинуть вертикальное окно на строку вниз. */
    private fun slide(y: Int) {
        if (y == 0) {
            for (row in 0..minOf(radius, height - 1)) accumulate(row, sign = 1)
            return
        }

        val entering = y + radius
        if (entering < height) accumulate(entering, sign = 1)
        val leaving = y - radius - 1
        if (leaving >= 0) accumulate(leaving, sign = -1)
    }

    private fun accumulate(row: Int, sign: Int) {
        val line = row * width
        for (x in 0 until width) {
            val value = samples[line + x].toInt() and BYTE_MASK
            columnSum[x] += sign * value
            columnSquares[x] += sign * value * value
        }
        rowsInWindow += sign
    }

    /** Прокатить горизонтальное окно по строке и раздать пикселям порог. */
    private fun threshold(y: Int, mask: ByteArray) {
        val last = width - 1
        var columns = minOf(radius, last) + 1
        var sum = 0L
        var squares = 0L
        for (x in 0 until columns) {
            sum += columnSum[x]
            squares += columnSquares[x]
        }

        val line = y * width
        for (x in 0..last) {
            if (x > 0) {
                val entering = x + radius
                if (entering <= last) {
                    sum += columnSum[entering]
                    squares += columnSquares[entering]
                    columns += 1
                }
                val leaving = x - radius - 1
                if (leaving >= 0) {
                    sum -= columnSum[leaving]
                    squares -= columnSquares[leaving]
                    columns -= 1
                }
            }

            val count = (columns * rowsInWindow).toDouble()
            val mean = sum / count
            val deviation = sqrt(max(0.0, squares / count - mean * mean))
            val limit = mean * (1 + WEIGHT * (deviation / RANGE - 1))
            mask[line + x] = if ((samples[line + x].toInt() and BYTE_MASK) > limit) WHITE else BLACK
        }
    }

    private companion object {
        /**
         * Насколько сильно разброс в окне снижает порог. 0.2 — значение
         * из исходной статьи: меньше — на бумаге остаётся шум, больше —
         * тонкие штрихи букв рвутся.
         */
        const val WEIGHT = 0.2

        /** Половина шкалы яркости, она же нормировка разброса. */
        const val RANGE = 128.0

        const val BYTE_MASK = 0xFF
        const val WHITE = 255.toByte()
        const val BLACK = 0.toByte()
    }
}
