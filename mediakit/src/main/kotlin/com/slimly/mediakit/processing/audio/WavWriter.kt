package com.slimly.mediakit.processing.audio

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Заголовок RIFF своими руками: писать WAV система не умеет.
 *
 * `MediaMuxer` знает только MP4-подобные контейнеры, а WAV — это сырые
 * отсчёты с 44-байтовой шапкой. Шапка пишется дважды: сначала с нулевыми
 * длинами, потом, когда весь звук уже на диске, — с настоящими.
 */
internal class WavWriter(private val sampleRate: Int, private val channels: Int) {
    /** Шапка фиксированной длины: место под неё резервируется до записи звука. */
    fun writeHeader(output: OutputStream, dataByteCount: Int = 0) {
        val bytesPerFrame = channels * BYTES_PER_SAMPLE
        val header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(dataByteCount + HEADER_SIZE - CHUNK_HEADER)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))

        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(FMT_SIZE)
        header.putShort(PCM_FORMAT)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(sampleRate * bytesPerFrame)
        header.putShort(bytesPerFrame.toShort())
        header.putShort((BYTES_PER_SAMPLE * BITS_IN_BYTE).toShort())

        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(dataByteCount)

        output.write(header.array())
    }

    companion object {
        const val HEADER_SIZE = 44

        /** Слово «RIFF» и длина за ним в размер, который она объявляет, не входят. */
        private const val CHUNK_HEADER = 8
        private const val FMT_SIZE = 16
        private const val PCM_FORMAT: Short = 1
        private const val BYTES_PER_SAMPLE = 2
        private const val BITS_IN_BYTE = 8
    }
}
