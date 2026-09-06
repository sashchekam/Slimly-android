package com.slimly.mediakit.processing.preview

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import java.io.File

/**
 * Кадр из готового файла одного вида.
 *
 * По воркеру на вид, а не одна ветвистая функция: у снимка, ролика
 * и документа общего только подпись — дальше это три разных системных
 * читателя со своими правилами закрытия.
 */
internal fun interface OutputFraming {
    /** `null` — кадра нет: файл битый или показывать в нём нечего. */
    fun frame(file: File, maxPixel: Int): Bitmap?
}

/**
 * Кадр снимка. `ImageDecoder` сам разворачивает картинку по EXIF и сам
 * уменьшает её при разборе — полный кадр в память не поднимается.
 */
internal class ImageOutputFramer : OutputFraming {
    override fun frame(file: File, maxPixel: Int): Bitmap? = runCatching {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > maxPixel) decoder.setTargetSampleSize(sampleSize(longest, maxPixel))
            // Кадр уедет в Compose как `ImageBitmap`, а тот аппаратный растр
            // читать не умеет: рисовать его можно, а трогать пиксели — нет.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }.getOrNull()

    /**
     * Степень уменьшения — ближайшая снизу степень двойки: `ImageDecoder`
     * округляет её сам, но округляет вверх, и кадр вышел бы мельче обещанного.
     */
    private fun sampleSize(longest: Int, maxPixel: Int): Int {
        var size = 1
        while (longest / (size * 2) >= maxPixel) size *= 2
        return size
    }
}

/** Кадр ролика — первый, а не средний: по нему ролик и узнают в списке. */
internal class VideoOutputFramer : OutputFraming {
    override fun frame(file: File, maxPixel: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.path)
            retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxPixel, maxPixel)
        } catch (@Suppress("TooGenericExceptionCaught") _: RuntimeException) {
            // Системный читатель бросает `IllegalArgumentException` и на битый
            // файл, и на формат, которого он не знает: разбирать эти случаи
            // порознь незачем — показать нечего в обоих.
            null
        } finally {
            retriever.release()
        }
    }
}

/**
 * Первая страница документа. Остальные не рисуем: просмотр показывает,
 * что документ собрался, а не заменяет собой читалку.
 */
internal class DocumentOutputFramer : OutputFraming {
    override fun frame(file: File, maxPixel: Int): Bitmap? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { document ->
                if (document.pageCount == 0) return null

                document.openPage(0).use { page ->
                    val scale = maxPixel.toFloat() / maxOf(page.width, page.height)
                    val bitmap = createBitmap(side(page.width, scale), side(page.height, scale))
                    // Белый фон обязателен: страница рисуется поверх того,
                    // что в растре уже есть, а незакрашенный растр прозрачен —
                    // чёрный текст лёг бы на чёрный фон экрана.
                    bitmap.eraseColor(PAGE_BACKGROUND)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }.getOrNull()

    /** Ноль пикселей растр не примет, а страница в один пиксель шириной бывает. */
    private fun side(value: Int, scale: Float): Int = (value * scale).toInt().coerceAtLeast(1)

    private companion object {
        const val PAGE_BACKGROUND = Color.WHITE
    }
}
