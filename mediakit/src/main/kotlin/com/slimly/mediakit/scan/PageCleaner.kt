package com.slimly.mediakit.scan

import android.graphics.Bitmap
import android.graphics.Color
import com.slimly.core.model.ScanMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Привести страницу к тому виду, какого от скана ждут: ровный белый фон,
 * резкий край буквы и плотные чернила.
 */
internal fun interface PageCleaning {
    fun cleaned(page: Bitmap, mode: ScanMode): Bitmap
}

/**
 * Четыре шага: выравнивание фона, обесцвечивание, маска нерезкости и кривая
 * плотности чернил.
 *
 * Без последнего шага страница остаётся фотографией текста: фон белый,
 * а буквы серые и мягкие. Именно это отличает снимок от скана — не фон,
 * а то, сколько в букве чёрного.
 *
 * На iOS первый шаг делает `CIDocumentEnhancer`, которого здесь нет вовсе.
 * Его работу — снять тень и серизну бумаги — берёт на себя деление
 * на сильно размытую копию кадра: там, где страница затенена, размытие
 * тоже темнее, и частное выходит ровным. Приём старый и делает ровно то же,
 * просто руками.
 */
internal class PageCleaner : PageCleaning {
    override fun cleaned(page: Bitmap, mode: ScanMode): Bitmap {
        val width = page.width
        val height = page.height
        if (width < 1 || height < 1) return page

        val pixels = IntArray(width * height)
        page.getPixels(pixels, 0, width, 0, 0, width, height)

        val luma = luminance(pixels)
        flatten(pixels, luma, width, height)
        if (mode.isMonochrome) desaturate(pixels)
        sharpen(pixels, width, height)
        mode.inkCurve?.let { ink(pixels, it) }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    // MARK: Фон

    /** Яркость по BT.601 — та же формула, по которой считается серый канал. */
    private fun luminance(pixels: IntArray): ByteArray = ByteArray(pixels.size) { index ->
        val pixel = pixels[index]
        val value = (RED * Color.red(pixel) + GREEN * Color.green(pixel) + BLUE * Color.blue(pixel)).toInt()
        value.coerceIn(0, MAX).toByte()
    }

    /**
     * Разделить кадр на его размытую копию: тень уходит, бумага становится
     * ровно белой.
     *
     * Радиус размытия — доля длинной стороны: окно должно быть заведомо шире
     * буквы, иначе буква делится сама на себя и бледнеет.
     */
    private fun flatten(pixels: IntArray, luma: ByteArray, width: Int, height: Int) {
        val radius = max(MINIMUM_BACKGROUND, (max(width, height) * BACKGROUND_RADIUS).roundToInt())
        val background = boxBlur(luma, width, height, radius)

        for (index in pixels.indices) {
            val paper = (background[index].toInt() and BYTE_MASK).coerceAtLeast(1)
            val pixel = pixels[index]
            pixels[index] = Color.argb(
                Color.alpha(pixel),
                scale(Color.red(pixel), paper),
                scale(Color.green(pixel), paper),
                scale(Color.blue(pixel), paper),
            )
        }
    }

    private fun scale(channel: Int, paper: Int): Int = (channel * MAX / paper).coerceIn(0, MAX)

    // MARK: Цвет

    private fun desaturate(pixels: IntArray) {
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val gray = (RED * Color.red(pixel) + GREEN * Color.green(pixel) + BLUE * Color.blue(pixel))
                .toInt()
                .coerceIn(0, MAX)
            pixels[index] = Color.argb(Color.alpha(pixel), gray, gray, gray)
        }
    }

    // MARK: Резкость

    /**
     * Маска нерезкости: разница кадра и его лёгкого размытия, добавленная
     * обратно к кадру.
     *
     * Страницу размывает не один шаг, а все сразу: объектив телефона, JPEG
     * камеры, выпрямление перспективы и пересчёт размера. Каждый съедает
     * по чуть-чуть, а вместе они и дают мягкий край буквы.
     */
    private fun sharpen(pixels: IntArray, width: Int, height: Int) {
        val radius = max(MINIMUM_SHARPEN, (max(width, height) * SHARPEN_RADIUS).roundToInt())
        val luma = luminance(pixels)
        val blurred = boxBlur(luma, width, height, radius)

        for (index in pixels.indices) {
            val delta = ((luma[index].toInt() and BYTE_MASK) - (blurred[index].toInt() and BYTE_MASK)) * SHARPEN_AMOUNT
            val pixel = pixels[index]
            pixels[index] = Color.argb(
                Color.alpha(pixel),
                (Color.red(pixel) + delta).roundToInt().coerceIn(0, MAX),
                (Color.green(pixel) + delta).roundToInt().coerceIn(0, MAX),
                (Color.blue(pixel) + delta).roundToInt().coerceIn(0, MAX),
            )
        }
    }

    // MARK: Плотность чернил

    /**
     * Кривая задаётся таблицей на 256 ступеней и накладывается на каждый
     * канал: считать степень для каждого пикселя вчетверо дороже, а ответов
     * всего 256.
     */
    private fun ink(pixels: IntArray, curve: InkCurve) {
        val table = IntArray(MAX + 1) { step ->
            val value = step.toFloat() / MAX
            val density = ((curve.paper - value) / (curve.paper - curve.ink)).coerceIn(0f, 1f)
            ((1 - density.pow(curve.gamma)) * MAX).roundToInt().coerceIn(0, MAX)
        }

        for (index in pixels.indices) {
            val pixel = pixels[index]
            pixels[index] = Color.argb(
                Color.alpha(pixel),
                table[Color.red(pixel)],
                table[Color.green(pixel)],
                table[Color.blue(pixel)],
            )
        }
    }

    /**
     * Размытие скользящим окном: два прохода — по строкам и по столбцам.
     *
     * Суммы в окне не пересчитываются, поэтому работы на пиксель одинаково
     * мало при любом радиусе. Именно это и позволяет держать радиус фона
     * в сотню пикселей, не считая страницу минуту.
     */
    private fun boxBlur(source: ByteArray, width: Int, height: Int, radius: Int): ByteArray {
        val horizontal = ByteArray(source.size)
        blurRows(source, horizontal, width, height, radius)

        val transposed = ByteArray(source.size)
        transpose(horizontal, transposed, width, height)

        val vertical = ByteArray(source.size)
        blurRows(transposed, vertical, height, width, radius)

        val result = ByteArray(source.size)
        transpose(vertical, result, height, width)
        return result
    }

    private fun blurRows(source: ByteArray, target: ByteArray, width: Int, height: Int, radius: Int) {
        for (y in 0 until height) {
            val line = y * width
            var sum = 0
            var count = 0

            for (x in 0..min(radius, width - 1)) {
                sum += source[line + x].toInt() and BYTE_MASK
                count += 1
            }

            for (x in 0 until width) {
                if (x > 0) {
                    val entering = x + radius
                    if (entering < width) {
                        sum += source[line + entering].toInt() and BYTE_MASK
                        count += 1
                    }
                    val leaving = x - radius - 1
                    if (leaving >= 0) {
                        sum -= source[line + leaving].toInt() and BYTE_MASK
                        count -= 1
                    }
                }
                target[line + x] = (sum / count).toByte()
            }
        }
    }

    private fun transpose(source: ByteArray, target: ByteArray, width: Int, height: Int) {
        for (y in 0 until height) {
            val line = y * width
            for (x in 0 until width) {
                target[x * height + y] = source[line + x]
            }
        }
    }

    private companion object {
        const val MAX = 255
        const val BYTE_MASK = 0xFF
        const val RED = 0.299f
        const val GREEN = 0.587f
        const val BLUE = 0.114f

        /** Окно фона — примерно десятая доля страницы: заведомо шире буквы. */
        const val BACKGROUND_RADIUS = 0.08f
        const val MINIMUM_BACKGROUND = 8

        /**
         * Радиус резкости привязан к размеру страницы, а не задан числом:
         * на четырёхстах dpi штрих буквы шире, чем на трёхстах, и один
         * и тот же радиус в пикселях означал бы там и там разное.
         */
        const val SHARPEN_RADIUS = 1f / 1750
        const val MINIMUM_SHARPEN = 1

        /**
         * Сила подъёма. Ореол вокруг буквы появляется от радиуса, а не
         * от силы, и при радиусе в один штрих его ещё нет.
         */
        const val SHARPEN_AMOUNT = 1.0f
    }
}
