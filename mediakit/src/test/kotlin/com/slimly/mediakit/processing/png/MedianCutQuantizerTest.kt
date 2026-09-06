package com.slimly.mediakit.processing.png

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Разложение кадра по палитре — арифметика над байтами: растр здесь
 * собирается числами, `Bitmap` для проверки не нужен.
 */
class MedianCutQuantizerTest {
    private val quantizer = MedianCutQuantizer()

    @Test
    fun `плоская графика ложится в палитру без единого изменения`() {
        val colors = listOf(
            PackedColor.of(255, 0, 0, 255),
            PackedColor.of(0, 255, 0, 255),
            PackedColor.of(0, 0, 255, 255),
            PackedColor.of(255, 255, 255, 255),
        )
        val raster = raster(width = 4, height = 4) { x, _ -> colors[x] }

        val indexed = quantizer.indexed(raster, colors = 256)

        assertEquals(colors.size, indexed.palette.size)
        assertEquals(colors.toSet(), indexed.palette.toSet())
        // Каждый пиксель нашёл в палитре ровно свой цвет.
        for (pixel in 0 until raster.pixelCount) {
            val index = indexed.indices[pixel].toInt() and 0xFF
            assertEquals(raster.color(pixel), indexed.palette[index])
        }
    }

    @Test
    fun `палитра не длиннее заказанной`() {
        // Плавный градиент: цветов в кадре куда больше, чем влезет.
        val raster = raster(width = 64, height = 64) { x, y -> PackedColor.of(x * 4, y * 4, 128, 255) }

        val indexed = quantizer.indexed(raster, colors = 16)

        assertTrue("Палитра из ${indexed.palette.size} цветов", indexed.palette.size <= 16)
        assertEquals(raster.pixelCount, indexed.indices.size)
        assertTrue(indexed.indices.all { (it.toInt() and 0xFF) < indexed.palette.size })
    }

    @Test
    fun `заказ вне границ формата приводится к ним`() {
        val raster = raster(width = 32, height = 32) { x, y -> PackedColor.of(x * 8, y * 8, x * y % 256, 255) }

        assertTrue(quantizer.indexed(raster, colors = 1).palette.size <= 2)
        assertTrue(quantizer.indexed(raster, colors = 1000).palette.size <= 256)
    }

    @Test
    fun `прозрачность не размазывается по кадру`() {
        // Половина кадра дырявая: альфа обязана остаться ровно такой,
        // какой была, — иначе дырку затянет полупрозрачной плёнкой.
        val raster = raster(width = 32, height = 32) { x, _ ->
            if (x < 16) PackedColor.of(200, 30, 30, 0) else PackedColor.of(200, 30, 30, 255)
        }

        val indexed = quantizer.indexed(raster, colors = 8)
        val alphas = indexed.palette.map { it.alpha }.toSet()

        assertEquals(setOf(0, 255), alphas)
    }

    private fun raster(width: Int, height: Int, color: (Int, Int) -> PackedColor): RgbaRaster {
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) pixels[y * width + x] = color(x, y).bits
        }
        return RgbaRaster(width, height, pixels)
    }
}
