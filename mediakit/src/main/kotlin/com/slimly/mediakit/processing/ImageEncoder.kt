package com.slimly.mediakit.processing

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import com.slimly.core.model.ImageFormat
import com.slimly.mediakit.processing.png.PalettePacker
import com.slimly.mediakit.processing.png.PalettePacking
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

internal data class ImageEncodingSettings(
    val format: ImageFormat,
    /** 0…100. У PNG не используется — он без потерь. */
    val quality: Int,
    /** Ограничение длинной стороны в пикселях. `null` — размер не трогаем. */
    val maxPixelSize: Int?,
    /**
     * Длина палитры для PNG. `null` — палитру не собираем и пишем truecolor:
     * приближение цветов уместно при сжатии, но не при смене формата, где
     * от PNG ждут точной копии кадра.
     */
    val paletteColors: Int? = null,
)

/** Перекодирование изображения из файла в файл. */
internal fun interface ImageEncoding {
    fun write(source: File, destination: File, settings: ImageEncodingSettings)
}

/**
 * Разбор через `ImageDecoder`, запись через `Bitmap.compress`.
 *
 * `ImageDecoder`, а не `BitmapFactory`: он сам разворачивает кадр по EXIF
 * и сам уменьшает его при разборе — иначе снимок с портретной ориентацией
 * вышел бы лежащим на боку, а полный растр 12 Мп прошёл бы через память
 * целиком ради того, чтобы тут же ужаться.
 *
 * `Bitmap` — растр без метаданных, поэтому после записи EXIF переносится
 * отдельным воркером: дату съёмки и геометку при сжатии терять нельзя.
 * Там же и палитра PNG — единственный рычаг веса у формата без потерь.
 */
internal class ImageEncoder(
    private val palette: PalettePacking = PalettePacker(),
    private val exif: ExifTransferring = ExifTransfer(),
) : ImageEncoding {
    override fun write(source: File, destination: File, settings: ImageEncodingSettings) {
        val bitmap = decode(source, settings.maxPixelSize)

        try {
            destination.outputStream().use { output ->
                val written = bitmap.compress(compressFormat(settings.format), settings.quality, output)
                if (!written) throw ProcessingError.UnsupportedFormat(settings.format.displayName)
            }
            packPalette(bitmap, destination, settings)
        } catch (error: IOException) {
            throw ProcessingError.WriteFailed(error.message.orEmpty(), error)
        } finally {
            bitmap.recycle()
        }

        // Последним шагом: палитровая запись собирает файл заново
        // и метаданных в нём не оставляет.
        exif.transfer(source, destination)
    }

    /**
     * Палитра выигрывает не всегда: на фотографии индексы приближённых цветов
     * весят больше честного truecolor. Поэтому она заменяет обычную запись
     * только тогда, когда результат действительно легче, а не «потому что PNG».
     */
    private fun packPalette(bitmap: Bitmap, destination: File, settings: ImageEncodingSettings) {
        if (settings.format != ImageFormat.PNG) return
        val colors = settings.paletteColors ?: return

        val packed = palette.pack(bitmap, colors) ?: return
        if (packed.size < destination.length()) destination.writeBytes(packed)
    }

    private fun decode(file: File, maxPixelSize: Int?): Bitmap = try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            // Растр должен лежать в памяти приложения: аппаратный кадр
            // `compress` записать не сможет.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE

            val longest = maxOf(info.size.width, info.size.height)
            if (maxPixelSize != null && longest > maxPixelSize) {
                val scale = maxPixelSize.toDouble() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }
    } catch (error: IOException) {
        throw ProcessingError.UnreadableSource(error)
    }

    /**
     * HEIC записать нечем: `Bitmap.compress` его не знает, а `HeifWriter`
     * работает только там, где у устройства есть аппаратный кодировщик.
     * Поэтому снимок HEIC при сжатии становится JPEG — единственный случай,
     * когда сжатие меняет формат, и он виден в имени готового файла.
     */
    private fun compressFormat(format: ImageFormat): Bitmap.CompressFormat = when (format) {
        ImageFormat.PNG -> Bitmap.CompressFormat.PNG

        ImageFormat.JPEG, ImageFormat.HEIC -> Bitmap.CompressFormat.JPEG

        ImageFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }
}
