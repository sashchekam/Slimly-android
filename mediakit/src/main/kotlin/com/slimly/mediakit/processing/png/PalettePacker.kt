package com.slimly.mediakit.processing.png

import android.graphics.Bitmap

/** Пережатие кадра в PNG с палитрой. */
internal fun interface PalettePacking {
    /**
     * @param colors длина палитры, 2…256.
     * @return `null`, если кадр не развернулся в растр или палитра не собралась.
     */
    fun pack(bitmap: Bitmap, colors: Int): ByteArray?
}

/**
 * У PNG нет качества: формат без потерь, и параметр `quality`
 * у `Bitmap.compress` для него ничего не значит. Единственное, чем в нём
 * можно управлять весом, — длина палитры. Индекс занимает байт вместо
 * четырёх, а повторяющиеся индексы deflate жмёт куда лучше, чем шум
 * truecolor.
 */
internal class PalettePacker(
    private val quantizer: ColorQuantizing = MedianCutQuantizer(),
    private val writer: IndexedPngWriter = IndexedPngWriter(),
) : PalettePacking {
    override fun pack(bitmap: Bitmap, colors: Int): ByteArray? {
        val raster = RgbaRaster.of(bitmap) ?: return null
        return writer.bytes(quantizer.indexed(raster, colors))
    }
}
