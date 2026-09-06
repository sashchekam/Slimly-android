package com.slimly.mediakit.processing.video

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

class VideoBitrateEstimatorTest {
    private val estimator = VideoBitrateEstimator()

    /** Файла на диске нет — значит, в ход идёт битрейт, сообщённый системой. */
    private val missing = File("нет-такого-файла.mp4")

    @Test
    fun `битрейт результата — доля исходного`() {
        val bitrate = estimator.bitrate(missing, sourceBitrate = 10_000_000, duration = 60.seconds, factor = 0.4)

        assertEquals(4_000_000, bitrate)
    }

    @Test
    fun `ниже дна не опускаемся`() {
        val bitrate = estimator.bitrate(missing, sourceBitrate = 500_000, duration = 60.seconds, factor = 0.1)

        assertEquals(400_000, bitrate)
    }

    @Test
    fun `без битрейта и без файла берём битрейт обычного ролика`() {
        val bitrate = estimator.bitrate(missing, sourceBitrate = 0, duration = 60.seconds, factor = 0.5)

        assertEquals(4_000_000, bitrate)
    }
}
