package com.slimly.mediakit.processing.png

import android.graphics.Bitmap

/**
 * Цвет одним словом — в том же порядке байтов, в каком его отдаёт `Bitmap`.
 *
 * Упакованным он дёшево ложится и в множество, и в ключ таблицы поиска,
 * а на неё при разложении кадра по палитре приходится по обращению
 * на каждый пиксель.
 */
@JvmInline
internal value class PackedColor(val bits: Int) {
    val alpha: Int
        get() = (bits ushr ALPHA_SHIFT) and BYTE

    val red: Int
        get() = (bits ushr RED_SHIFT) and BYTE

    val green: Int
        get() = (bits ushr GREEN_SHIFT) and BYTE

    val blue: Int
        get() = bits and BYTE

    val isOpaque: Boolean
        get() = alpha == BYTE

    /**
     * Канал по номеру — так по осям ходит медианное сечение, которому всё
     * равно, какой именно канал оказался самым длинным.
     */
    fun component(axis: Int): Int = when (axis) {
        RED -> red
        GREEN -> green
        BLUE -> blue
        else -> alpha
    }

    companion object {
        const val RED = 0
        const val GREEN = 1
        const val BLUE = 2
        const val ALPHA = 3
        const val CHANNELS = 4

        fun of(red: Int, green: Int, blue: Int, alpha: Int): PackedColor =
            PackedColor((alpha shl ALPHA_SHIFT) or (red shl RED_SHIFT) or (green shl GREEN_SHIFT) or blue)

        private const val BYTE = 0xFF
        private const val ALPHA_SHIFT = 24
        private const val RED_SHIFT = 16
        private const val GREEN_SHIFT = 8
    }
}

/**
 * Развёрнутый кадр: слово на пиксель, строки сверху вниз.
 *
 * Альфа прямая, не предумноженная: `Bitmap.getPixels` отдаёт именно такую,
 * а в индексированном PNG она хранится отдельным чанком, и вмешивать её
 * в цвет нельзя.
 */
internal class RgbaRaster(val width: Int, val height: Int, val pixels: IntArray) {
    val pixelCount: Int
        get() = width * height

    fun color(at: Int): PackedColor = PackedColor(pixels[at])

    companion object {
        /** `null` — кадра нет: пустой растр палитре не разложить. */
        fun of(bitmap: Bitmap): RgbaRaster? {
            val width = bitmap.width
            val height = bitmap.height
            if (width <= 0 || height <= 0) return null

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            return RgbaRaster(width = width, height = height, pixels = pixels)
        }
    }
}
