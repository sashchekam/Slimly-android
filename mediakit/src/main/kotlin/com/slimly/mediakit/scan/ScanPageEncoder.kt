package com.slimly.mediakit.scan

import android.graphics.Bitmap
import android.graphics.Color
import com.slimly.core.model.ScanMode
import com.slimly.mediakit.processing.ProcessingError
import java.io.ByteArrayOutputStream

/** Чистая картинка страницы → страница в том виде, в каком она ляжет в PDF. */
internal fun interface ScanPageEncoding {
    fun page(image: Bitmap, mode: ScanMode): ScannedPage
}

internal class ScanPageEncoder(private val binarizer: PageBinarizing = SauvolaBinarizer()) : ScanPageEncoding {
    /**
     * На двухцветной странице JPEG работает против себя: он растит вес
     * на резких краях букв — ровно там, где у ч/б скана всё содержание.
     */
    override fun page(image: Bitmap, mode: ScanMode): ScannedPage {
        if (mode == ScanMode.BLACK_AND_WHITE) return binarizer.bitmap(grayscale(image))

        val bytes = ByteArrayOutputStream()
        if (!image.compress(Bitmap.CompressFormat.JPEG, mode.jpegQuality, bytes)) {
            throw ProcessingError.PageEncodingFailed()
        }

        return ScannedPage.Jpeg(
            data = bytes.toByteArray(),
            width = image.width,
            height = image.height,
            // Серую страницу PDF примет одним каналом, но сам JPEG система
            // всё равно пишет тремя: цветовое пространство берём по факту,
            // а не по режиму, иначе документ покажет мусор.
            isGray = false,
        )
    }

    /** Растр в полутоновый буфер: порогу нужны соседи пикселя, а не цвет. */
    private fun grayscale(image: Bitmap): GrayscaleBuffer {
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)

        val samples = ByteArray(pixels.size)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val value = RED * Color.red(pixel) + GREEN * Color.green(pixel) + BLUE * Color.blue(pixel)
            samples[index] = value.toInt().coerceIn(0, MAX).toByte()
        }

        return GrayscaleBuffer(width, height, samples)
    }

    private companion object {
        const val MAX = 255
        const val RED = 0.299f
        const val GREEN = 0.587f
        const val BLUE = 0.114f
    }
}
