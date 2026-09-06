package com.slimly.mediakit.processing.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer

/**
 * Дорожка в WAV: распаковываем сжатый звук и пишем отсчёты как есть.
 *
 * В отличие от копии дорожки здесь без декодера не обойтись — WAV хранит
 * сырой PCM, а в контейнере лежит AAC. Кодировщика при этом всё равно нет:
 * из декодера отсчёты уходят прямо в файл.
 */
internal fun interface AudioDecoding {
    suspend fun decode(source: File, destination: File, progress: (Double) -> Unit)
}

internal class AudioDecoder : AudioDecoding {
    override suspend fun decode(source: File, destination: File, progress: (Double) -> Unit) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(source.path)
            val track = audioTrack(extractor) ?: throw MissingAudioTrack()
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)

            val mime = requireNotNull(format.getString(MediaFormat.KEY_MIME))
            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val written = destination.outputStream().buffered().use { output ->
                val writer = WavWriter(
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                )
                // Место под шапку резервируем нулями: настоящие длины
                // известны только когда звук уже на диске.
                writer.writeHeader(output)
                drain(extractor, codec, output, format.longOrNull(MediaFormat.KEY_DURATION) ?: 0L, progress)
            }

            rewriteHeader(destination, format, written)
            progress(1.0)
        } finally {
            runCatching { codec?.stop() }
            codec?.release()
            extractor.release()
        }
    }

    /** Прокачивает пакеты через декодер, пока дорожка не кончится. */
    @Suppress("LongParameterList")
    private fun drain(
        extractor: MediaExtractor,
        codec: MediaCodec,
        output: OutputStream,
        duration: Long,
        progress: (Double) -> Unit,
    ): Int {
        val info = MediaCodec.BufferInfo()
        var written = 0
        var isInputDone = false
        var isOutputDone = false

        while (!isOutputDone) {
            if (!isInputDone) isInputDone = feed(extractor, codec)

            val index = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (index < 0) continue

            written += take(codec, index, info, output)
            if (duration > 0) progress(info.presentationTimeUs.toDouble() / duration)
            isOutputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
        }

        return written
    }

    /** Готовые отсчёты из декодера — в файл. Буфер возвращаем в любом случае. */
    private fun take(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo, output: OutputStream): Int {
        val buffer = codec.getOutputBuffer(index)
        val written = if (buffer != null && info.size > 0) write(buffer, info, output) else 0
        codec.releaseOutputBuffer(index, false)
        return written
    }

    /** @return `true`, когда исходник кончился и в декодер отправлен признак конца. */
    private fun feed(extractor: MediaExtractor, codec: MediaCodec): Boolean {
        val index = codec.dequeueInputBuffer(TIMEOUT_US)
        if (index < 0) return false

        val buffer = codec.getInputBuffer(index) ?: return false
        val size = extractor.readSampleData(buffer, 0)
        val isDone = size < 0

        if (isDone) {
            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        } else {
            codec.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
            extractor.advance()
        }
        return isDone
    }

    private fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo, output: OutputStream): Int {
        val chunk = ByteArray(info.size)
        buffer.position(info.offset)
        buffer.get(chunk)
        output.write(chunk)
        return chunk.size
    }

    /**
     * Настоящие длины в уже записанный файл. Перезапись шапки дешевле, чем
     * держать весь распакованный звук в памяти ради одного числа: час записи
     * в PCM — это шестьсот мегабайт.
     */
    private fun rewriteHeader(destination: File, format: MediaFormat, dataByteCount: Int) {
        RandomAccessFile(destination, "rw").use { file ->
            val header = ByteArrayOutputStream()
            WavWriter(
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
            ).writeHeader(header, dataByteCount)

            file.seek(0)
            file.write(header.toByteArray())
        }
    }

    private fun audioTrack(extractor: MediaExtractor): Int? = (0 until extractor.trackCount).firstOrNull { index ->
        extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
    }

    private companion object {
        /** Десять миллисекунд: столько ждём буфер, прежде чем заняться другим концом. */
        const val TIMEOUT_US = 10_000L
    }
}
