package com.slimly.mediakit.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Порог Саувола и упаковка по биту на пиксель — чистая арифметика над
 * байтами, и проверяется она без устройства.
 */
class SauvolaBinarizerTest {
    private val binarizer = SauvolaBinarizer()

    @Test
    fun `ровное поле бумаги остаётся белым`() {
        val page = binarizer.bitmap(buffer(SIDE, SIDE) { _, _ -> PAPER })

        // Ровно тот случай, ради которого в пороге стоит поправка
        // на разброс: без неё половина шума на пустом поле уходит в чернила.
        assertTrue((0 until SIDE step 7).all { isWhite(page, x = it, y = it) })
    }

    @Test
    fun `штрих чернил на бумаге становится чёрным`() {
        val page = binarizer.bitmap(
            buffer(SIDE, SIDE) { x, y -> if (y in 49..51) INK else PAPER },
        )

        assertFalse(isWhite(page, x = 50, y = 50))
        assertTrue(isWhite(page, x = 50, y = 5))
    }

    @Test
    fun `тень на половине страницы не съедает текст`() {
        // Левая половина затенена вдвое: одному порогу на всю страницу
        // она и стоила бы текста.
        val page = binarizer.bitmap(
            buffer(SIDE, SIDE) { x, y ->
                val paper = if (x < SIDE / 2) SHADOWED_PAPER else PAPER
                val ink = if (x < SIDE / 2) SHADOWED_INK else INK
                if (y in 49..51 && (x in 20..30 || x in 70..80)) ink else paper
            },
        )

        assertFalse(isWhite(page, x = 25, y = 50))
        assertFalse(isWhite(page, x = 75, y = 50))
        assertTrue(isWhite(page, x = 50, y = 10))
    }

    @Test
    fun `строка выровнена по байту, а лишние биты остаются нулями`() {
        val page = binarizer.bitmap(buffer(width = 12, height = 2) { _, _ -> PAPER })

        assertEquals(2, page.rowBytes)
        assertEquals(4, page.bits.size)
        // Двенадцать пикселей — это байт целиком и половина второго;
        // хвост байта остаётся нулями и в кадр не входит.
        assertEquals(FULL, page.bits[0])
        assertEquals(0xF0.toByte(), page.bits[1])
    }

    @Test
    fun `пустая страница не роняет разбор`() {
        val page = binarizer.bitmap(buffer(width = 0, height = 0) { _, _ -> PAPER })

        assertEquals(0, page.width)
        assertEquals(0, page.bits.size)
    }

    private fun buffer(width: Int, height: Int, value: (Int, Int) -> Int): GrayscaleBuffer {
        val samples = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) samples[y * width + x] = value(x, y).toByte()
        }
        return GrayscaleBuffer(width, height, samples)
    }

    private fun isWhite(page: ScannedPage.Bilevel, x: Int, y: Int): Boolean {
        val byte = page.bits[y * page.rowBytes + x / 8].toInt() and 0xFF
        return byte and (0x80 shr (x % 8)) != 0
    }

    private companion object {
        const val SIDE = 100
        const val PAPER = 230
        const val INK = 30
        const val SHADOWED_PAPER = 110
        const val SHADOWED_INK = 15
        val FULL = 0xFF.toByte()
    }
}
