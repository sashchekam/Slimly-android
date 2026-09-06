package com.slimly.mediakit.processing

import android.net.Uri
import com.slimly.core.model.CompressionLevel
import com.slimly.core.model.ImageFormat
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingResult
import com.slimly.core.model.VideoContainer
import com.slimly.mediakit.processing.video.VideoTranscodeSettings
import com.slimly.mediakit.processing.video.VideoTranscoding
import java.io.File

/** Сжатие одного файла. */
internal fun interface CompressionPerforming {
    suspend fun compress(
        item: MediaItem,
        source: Uri,
        level: CompressionLevel,
        progress: (Double) -> Unit,
    ): ProcessingResult
}

/**
 * Сжатие не меняет тип файла: PNG остаётся PNG, WebP — WebP, кодек видео
 * остаётся тем же. Иначе «сжать» незаметно превращалось бы
 * в «сконвертировать».
 *
 * Исключений два, и оба вынужденные. HEIC записать нечем, и снимок
 * становится JPEG — почему так, написано в [ImageEncoder]. Видео всегда
 * выходит в MP4: другого контейнера системный упаковщик не пишет, и ролик
 * из MKV вернётся в MP4 с той же дорожкой внутри.
 */
internal class CompressionWorker(
    private val videos: VideoTranscoding,
    private val results: ProcessingResultWriting,
    private val images: ImageEncoding = ImageEncoder(),
    private val settings: CompressionSettingsProviding = EncodingSettings(),
) : CompressionPerforming {
    override suspend fun compress(
        item: MediaItem,
        source: Uri,
        level: CompressionLevel,
        progress: (Double) -> Unit,
    ): ProcessingResult {
        val file = File(requireNotNull(source.path) { "Оригинал приходит файлом, а не записью медиатеки" })

        return when (item.kind) {
            MediaKind.PHOTO -> compressPhoto(item, file, level, progress)
            MediaKind.VIDEO -> compressVideo(item, file, level, progress)
        }
    }

    private suspend fun compressPhoto(
        item: MediaItem,
        source: File,
        level: CompressionLevel,
        progress: (Double) -> Unit,
    ): ProcessingResult {
        val format = format(item)

        return results.write(
            name = "${item.baseName}.${format.fileExtension}",
            item = item,
            destination = OutputDestination.GALLERY,
        ) { destination ->
            images.write(
                source = source,
                destination = destination,
                settings = ImageEncodingSettings(
                    format = format,
                    quality = settings.imageQuality(level),
                    maxPixelSize = settings.imageMaxPixelSize(level),
                    // Единственный рычаг веса у PNG: качество он игнорирует,
                    // и без палитры «сжатие PNG» свелось бы к уменьшению кадра.
                    paletteColors = settings.imagePaletteColors(level),
                ),
            )
            progress(1.0)
        }
    }

    private suspend fun compressVideo(
        item: MediaItem,
        source: File,
        level: CompressionLevel,
        progress: (Double) -> Unit,
    ): ProcessingResult = results.write(
        name = "${item.baseName}.${VideoContainer.MP4.fileExtension}",
        item = item,
        destination = OutputDestination.GALLERY,
    ) { destination ->
        videos.transcode(
            source = source,
            destination = destination,
            settings = VideoTranscodeSettings(
                // Кодек оставляем исходный: сжатие не меняет тип файла.
                codec = null,
                shortSideLimit = settings.videoShortSide(level),
                bitrateFactor = settings.videoBitrateFactor(level),
            ),
            progress = progress,
        )
    }

    /** Формат берём у самого файла: сжатие сохраняет тот, что был. */
    private fun format(item: MediaItem): ImageFormat = when (item.fileExtension) {
        "png" -> ImageFormat.PNG

        "webp" -> ImageFormat.WEBP

        // HEIC записывается как JPEG — и расширение должно об этом сказать.
        else -> ImageFormat.JPEG
    }
}
