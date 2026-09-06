package com.slimly.mediakit.processing

import android.net.Uri
import com.slimly.core.model.AudioFormat
import com.slimly.core.model.MediaFormat
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingResult
import com.slimly.core.model.ProcessingTarget
import com.slimly.mediakit.processing.audio.AudioDecoder
import com.slimly.mediakit.processing.audio.AudioDecoding
import com.slimly.mediakit.processing.audio.AudioTrackCopier
import com.slimly.mediakit.processing.audio.AudioTrackCopying
import com.slimly.mediakit.processing.audio.MissingAudioTrack
import java.io.File

/** Извлечение звуковой дорожки из видео. */
internal fun interface AudioExtracting {
    suspend fun extract(
        item: MediaItem,
        source: Uri,
        format: MediaFormat,
        progress: (Double) -> Unit,
    ): ProcessingResult
}

/**
 * Самый дешёвый инструмент из четырёх: M4A получается копией дорожки без
 * перекодирования, и работа сводится к перекладыванию пакетов. Платить
 * декодером приходится только за WAV — там нужен сырой PCM.
 *
 * Готовый файл уходит не в галерею, а в папку приложения: звуку в галерее
 * места нет, зато он виден в «Файлах».
 */
internal class AudioWorker(
    private val results: ProcessingResultWriting,
    private val copier: AudioTrackCopying = AudioTrackCopier(),
    private val decoder: AudioDecoding = AudioDecoder(),
) : AudioExtracting {
    /**
     * Проверки до работы: звук извлекается только из видео — у фотографии
     * дорожки нет, и сказать об этом надо до того, как система начнёт
     * её искать.
     */
    private fun audioTarget(item: MediaItem, format: MediaFormat): ProcessingTarget.Audio {
        if (item.kind != MediaKind.VIDEO) throw ProcessingError.AudioFromPhoto()
        return format.target as? ProcessingTarget.Audio ?: throw ProcessingError.UnsupportedOperation()
    }

    override suspend fun extract(
        item: MediaItem,
        source: Uri,
        format: MediaFormat,
        progress: (Double) -> Unit,
    ): ProcessingResult {
        val target = audioTarget(item, format)
        val file = File(requireNotNull(source.path) { "Оригинал приходит файлом, а не записью медиатеки" })

        return results.write(
            name = "${item.baseName}.${target.format.fileExtension}",
            item = item,
            destination = OutputDestination.FILES,
        ) { destination ->
            try {
                when (target.format) {
                    is AudioFormat.Aac -> copier.copy(file, destination, progress)
                    AudioFormat.LinearPcm -> decoder.decode(file, destination, progress)
                }
            } catch (error: MissingAudioTrack) {
                throw ProcessingError.NoAudioTrack(error)
            }
        }
    }
}
