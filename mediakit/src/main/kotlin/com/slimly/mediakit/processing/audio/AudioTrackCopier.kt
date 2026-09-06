package com.slimly.mediakit.processing.audio

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Копия звуковой дорожки из контейнера — без перекодирования.
 *
 * Здесь Android дешевле iOS: там дорожка всё равно проходит через ридер
 * и райтер, а `MediaExtractor` с `MediaMuxer` просто перекладывают уже
 * сжатые пакеты AAC в новый контейнер. Мгновенно и без потери качества.
 */
internal fun interface AudioTrackCopying {
    /** @return длительность дорожки в микросекундах — по ней считается прогресс. */
    suspend fun copy(source: File, destination: File, progress: (Double) -> Unit)
}

internal class AudioTrackCopier : AudioTrackCopying {
    override suspend fun copy(source: File, destination: File, progress: (Double) -> Unit) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(source.path)
            val track = audioTrack(extractor) ?: throw MissingAudioTrack()
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)

            muxer = MediaMuxer(destination.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val output = muxer.addTrack(format)
            muxer.start()

            val duration = format.longOrNull(MediaFormat.KEY_DURATION) ?: 0L
            val buffer = ByteBuffer.allocate(bufferSize(format))
            val info = android.media.MediaCodec.BufferInfo()

            while (true) {
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break

                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                info.offset = 0
                muxer.writeSampleData(output, buffer, info)

                if (duration > 0) progress(info.presentationTimeUs.toDouble() / duration)
                extractor.advance()
            }

            progress(1.0)
        } finally {
            extractor.release()
            // Останов может бросить, если не записано ни одного пакета,
            // — файл тогда всё равно негоден, и хоронить исходную ошибку
            // под этой нельзя.
            runCatching { muxer?.stop() }
            muxer?.release()
        }
    }

    private fun audioTrack(extractor: MediaExtractor): Int? = (0 until extractor.trackCount).firstOrNull { index ->
        extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
    }

    /**
     * Размер буфера берём из формата, а не наугад: у дорожки с высоким
     * битрейтом пакет не влезет в маленький, и чтение оборвётся на середине.
     */
    private fun bufferSize(format: MediaFormat): Int =
        format.longOrNull(MediaFormat.KEY_MAX_INPUT_SIZE)?.toInt()?.takeIf { it > 0 } ?: DEFAULT_BUFFER

    private companion object {
        const val DEFAULT_BUFFER = 256 * 1024
    }
}

/** Значение поля формата, которого может не быть: `MediaFormat` бросает на промахе. */
internal fun MediaFormat.longOrNull(key: String): Long? =
    if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

/** В файле нет звука — извлекать нечего. */
internal class MissingAudioTrack : Exception()
