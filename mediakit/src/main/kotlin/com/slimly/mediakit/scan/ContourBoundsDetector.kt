package com.slimly.mediakit.scan

import android.graphics.Bitmap
import android.graphics.Color
import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.QuadPoint
import com.slimly.core.service.DocumentBoundsDetector
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Поиск листа в кадре — своими руками.
 *
 * Готового `VNDetectDocumentSegmentationRequest`, как на iOS, в Android нет.
 * Развилка была между ML Kit Document Scanner и собственным поиском контура;
 * выбран второй: ML Kit приходит вместе со своим полноэкранным интерфейсом,
 * который нельзя ни покрасить в Nocturne, ни встроить в наш поток шагов.
 *
 * Приём простой и от этого предсказуемый: лист светлее стола, поэтому кадр
 * делится порогом Оцу на «бумагу» и «фон», из бумаги берётся самая крупная
 * связная область, а её четыре угла — крайние точки по суммам и разностям
 * координат. На белом листе поверх белого стола это не работает, и тогда
 * честнее вернуть `null`, чем обрезать наугад: человек поправит рамку сам.
 */
class ContourBoundsDetector : DocumentBoundsDetector {
    override fun bounds(frame: Bitmap): DocumentQuad? {
        val scaled = downscaled(frame)
        val width = scaled.width
        val height = scaled.height

        val luma = if (width >= MINIMUM_SIDE && height >= MINIMUM_SIDE) luminance(scaled) else null
        if (scaled !== frame) scaled.recycle()

        val region = luma
            ?.let { mask(it, threshold(it)) }
            ?.let { largestRegion(it, width, height) }
            ?: return null

        return corners(region, width, height).takeIf { it.isUsable && !it.coversEverything }
    }

    /**
     * Ищем на уменьшенной копии: лист занимает заметную часть кадра, а работы
     * при этом на два порядка меньше. Точность углов от этого не страдает —
     * рамку всё равно поправляют пальцем.
     */
    private fun downscaled(frame: Bitmap): Bitmap {
        val side = max(frame.width, frame.height)
        if (side <= WORKING_SIDE) return frame
        val scale = WORKING_SIDE.toFloat() / side
        return Bitmap.createScaledBitmap(
            frame,
            max(1, (frame.width * scale).roundToInt()),
            max(1, (frame.height * scale).roundToInt()),
            true,
        )
    }

    private fun luminance(frame: Bitmap): IntArray {
        val pixels = IntArray(frame.width * frame.height)
        frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
        return IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            (RED * Color.red(pixel) + GREEN * Color.green(pixel) + BLUE * Color.blue(pixel)).toInt().coerceIn(0, MAX)
        }
    }

    /**
     * Порог Оцу: делит гистограмму так, чтобы разброс внутри обеих половин
     * был наименьшим. Никаких настроек — кадр решает сам.
     */
    private fun threshold(luma: IntArray): Int {
        val histogram = IntArray(MAX + 1)
        luma.forEach { histogram[it] += 1 }

        val total = luma.size
        val sum = histogram.indices.sumOf { it.toLong() * histogram[it] }
        var backgroundSum = 0L
        var backgroundCount = 0
        var best = 0
        var bestVariance = -1.0

        for (level in 0..MAX) {
            backgroundCount += histogram[level]
            backgroundSum += level.toLong() * histogram[level]
            val foregroundCount = total - backgroundCount
            if (backgroundCount == 0 || foregroundCount == 0) continue

            val backgroundMean = backgroundSum.toDouble() / backgroundCount
            val foregroundMean = (sum - backgroundSum).toDouble() / foregroundCount
            val gap = backgroundMean - foregroundMean
            val variance = backgroundCount.toDouble() * foregroundCount * gap * gap

            if (variance > bestVariance) {
                bestVariance = variance
                best = level
            }
        }

        return best
    }

    /** Бумага — то, что светлее порога. `null` — светлого в кадре почти нет. */
    private fun mask(luma: IntArray, threshold: Int): BooleanArray? {
        val paper = BooleanArray(luma.size) { luma[it] > threshold }
        val count = paper.count { it }
        return paper.takeIf { count > luma.size * MINIMUM_SHARE }
    }

    /**
     * Самая крупная связная область светлого — сам лист.
     *
     * Обход в ширину по массиву-очереди, а не рекурсией: страница может
     * занять весь кадр, и рекурсия на сотне тысяч пикселей переполнит стек.
     */
    private fun largestRegion(paper: BooleanArray, width: Int, height: Int): List<Int>? {
        val visited = BooleanArray(paper.size)
        val queue = IntArray(paper.size)
        var largest: List<Int>? = null

        for (start in paper.indices) {
            if (paper[start] && !visited[start]) {
                val region = fill(start, paper, visited, queue, width, height)
                if (region.size > (largest?.size ?: 0)) largest = region
            }
        }

        return largest?.takeIf { it.size > paper.size * MINIMUM_SHARE }
    }

    /** Заливка от одной точки: всё светлое, до чего можно дойти по соседям. */
    @Suppress("LongParameterList")
    private fun fill(
        start: Int,
        paper: BooleanArray,
        visited: BooleanArray,
        queue: IntArray,
        width: Int,
        height: Int,
    ): List<Int> {
        var head = 0
        var tail = 0
        queue[tail++] = start
        visited[start] = true
        val region = mutableListOf<Int>()

        while (head < tail) {
            val index = queue[head++]
            region.add(index)
            val x = index % width
            val y = index / width

            NEIGHBOURS.forEach { (dx, dy) ->
                val nx = x + dx
                val ny = y + dy
                val next = if (nx in 0 until width && ny in 0 until height) ny * width + nx else null
                if (next != null && paper[next] && !visited[next]) {
                    visited[next] = true
                    queue[tail++] = next
                }
            }
        }

        return region
    }

    /**
     * Четыре угла области: крайние точки по сумме и разности координат.
     *
     * У повёрнутого прямоугольника минимум суммы — левый верхний угол,
     * максимум — правый нижний, а разность так же разводит два оставшихся.
     * Приём работает при любом наклоне и не требует ни выпуклой оболочки,
     * ни перебора.
     */
    private fun corners(region: List<Int>, width: Int, height: Int): DocumentQuad {
        var topLeft = region.first()
        var bottomRight = region.first()
        var topRight = region.first()
        var bottomLeft = region.first()

        fun sum(index: Int) = index % width + index / width
        fun diff(index: Int) = index % width - index / width

        region.forEach { index ->
            if (sum(index) < sum(topLeft)) topLeft = index
            if (sum(index) > sum(bottomRight)) bottomRight = index
            if (diff(index) > diff(topRight)) topRight = index
            if (diff(index) < diff(bottomLeft)) bottomLeft = index
        }

        fun point(index: Int) = QuadPoint(
            x = (index % width).toFloat() / width,
            y = (index / width).toFloat() / height,
        )

        return DocumentQuad(
            topLeft = point(topLeft),
            topRight = point(topRight),
            bottomRight = point(bottomRight),
            bottomLeft = point(bottomLeft),
        )
    }

    /**
     * Рамка по краю кадра — это не найденный лист, а не найденный фон:
     * предлагать её значит обещать обрезку, которой не будет.
     */
    private val DocumentQuad.coversEverything: Boolean
        get() = corners.all { it.x <= EDGE || it.x >= 1 - EDGE } && corners.all { it.y <= EDGE || it.y >= 1 - EDGE }

    private companion object {
        const val MAX = 255
        const val RED = 0.299f
        const val GREEN = 0.587f
        const val BLUE = 0.114f

        /** Длинная сторона рабочей копии кадра. */
        const val WORKING_SIDE = 320
        const val MINIMUM_SIDE = 16

        /** Меньше этой доли кадра — уже не лист, а блик или пятно света. */
        const val MINIMUM_SHARE = 0.08

        /** Насколько близко к краю угол считается «прижатым» к нему. */
        const val EDGE = 0.02f

        val NEIGHBOURS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    }
}
