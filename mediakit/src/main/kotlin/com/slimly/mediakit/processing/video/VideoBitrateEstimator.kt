package com.slimly.mediakit.processing.video

import java.io.File
import kotlin.time.Duration

/** Битрейт результата: доля от исходного, но не ниже разумного дна. */
internal fun interface VideoBitrateEstimating {
    /**
     * @param sourceBitrate битрейт исходной дорожки по данным системы.
     *   Ноль означает, что система его не сообщила.
     */
    fun bitrate(source: File, sourceBitrate: Int, duration: Duration, factor: Double): Int
}

internal class VideoBitrateEstimator : VideoBitrateEstimating {
    override fun bitrate(source: File, sourceBitrate: Int, duration: Duration, factor: Double): Int {
        val base = if (sourceBitrate > 1) sourceBitrate.toDouble() else fallback(source, duration)
        return maxOf(FLOOR, base * factor).toInt()
    }

    /** Битрейт по весу файла — на случай, если система его не сообщила. */
    private fun fallback(source: File, duration: Duration): Double {
        val seconds = duration.inWholeMilliseconds / MILLIS_IN_SECOND
        if (seconds <= 0) return UNKNOWN_BITRATE

        val size = source.length()
        if (size <= 0) return UNKNOWN_BITRATE

        return size * BITS_IN_BYTE / seconds
    }

    private companion object {
        /** Ниже 400 кбит/с картинка рассыпается независимо от разрешения. */
        const val FLOOR = 400_000.0

        /**
         * Битрейт «обычного» ролика: им подменяем неизвестный, когда
         * не выходит посчитать даже по весу файла.
         */
        const val UNKNOWN_BITRATE = 8_000_000.0

        const val MILLIS_IN_SECOND = 1000.0
        const val BITS_IN_BYTE = 8.0
    }
}
