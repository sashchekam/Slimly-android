package com.slimly.mediakit.processing

import android.net.Uri
import com.slimly.core.model.ImageFormat
import com.slimly.core.model.MediaFormat
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingResult
import com.slimly.core.model.ProcessingTarget
import com.slimly.core.model.VideoCodec
import com.slimly.core.model.VideoContainer
import com.slimly.mediakit.processing.video.VideoTranscodeSettings
import com.slimly.mediakit.processing.video.VideoTranscoding
import java.io.File

/** Смена формата одного файла. */
internal fun interface ConversionPerforming {
    suspend fun convert(
        item: MediaItem,
        source: Uri,
        format: MediaFormat,
        progress: (Double) -> Unit,
    ): ProcessingResult
}

internal class ConversionWorker(
    private val videos: VideoTranscoding,
    private val results: ProcessingResultWriting,
    private val images: ImageEncoding = ImageEncoder(),
) : ConversionPerforming {
    override suspend fun convert(
        item: MediaItem,
        source: Uri,
        format: MediaFormat,
        progress: (Double) -> Unit,
    ): ProcessingResult {
        val file = File(requireNotNull(source.path) { "Оригинал приходит файлом, а не записью медиатеки" })

        return when (val target = format.target) {
            is ProcessingTarget.Image -> convertImage(item, file, target.format, progress)

            is ProcessingTarget.Video -> convertVideo(item, file, target.codec, format.ratio, progress)

            // Звук — фаза 6, документ собирается не отсюда: страница
            // становится PDF вместе со всем документом, а не поштучно.
            else -> throw ProcessingError.UnsupportedOperation()
        }
    }

    private suspend fun convertImage(
        item: MediaItem,
        source: File,
        target: ImageFormat,
        progress: (Double) -> Unit,
    ): ProcessingResult {
        // Вид файла и вид формата обязаны совпасть: JPEG из ролика
        // не получится, и сказать об этом надо до кодировщика.
        if (item.kind != MediaKind.PHOTO) throw ProcessingError.VideoTarget(target.displayName)

        return results.write(
            name = "${item.baseName}.${target.fileExtension}",
            item = item,
            destination = OutputDestination.GALLERY,
        ) { destination ->
            images.write(
                source = source,
                destination = destination,
                settings = ImageEncodingSettings(
                    format = target,
                    quality = IMAGE_QUALITY,
                    // Смена формата не должна портить кадр: размер не трогаем.
                    maxPixelSize = null,
                ),
            )
            progress(1.0)
        }
    }

    private suspend fun convertVideo(
        item: MediaItem,
        source: File,
        codec: VideoCodec,
        ratio: Double,
        progress: (Double) -> Unit,
    ): ProcessingResult {
        if (item.kind != MediaKind.VIDEO) throw ProcessingError.PhotoTarget(codec.displayName)

        return results.write(
            name = "${item.baseName}.${VideoContainer.MP4.fileExtension}",
            item = item,
            destination = OutputDestination.GALLERY,
        ) { destination ->
            videos.transcode(
                source = source,
                destination = destination,
                settings = VideoTranscodeSettings(
                    codec = codec,
                    // Кадр остаётся как был: человек менял кодек, а не разрешение.
                    shortSideLimit = null,
                    // HEVC при том же качестве требует меньше битрейта — ровно
                    // на столько, сколько обещает оценка формата.
                    bitrateFactor = ratio,
                ),
                progress = progress,
            )
        }
    }

    private companion object {
        /** Смена формата не должна портить кадр: качество высокое. */
        const val IMAGE_QUALITY = 90
    }
}
