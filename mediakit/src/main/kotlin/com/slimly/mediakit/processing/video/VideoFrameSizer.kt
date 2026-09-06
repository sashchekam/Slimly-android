package com.slimly.mediakit.processing.video

import kotlin.math.roundToInt

internal data class VideoFrameSize(val width: Int, val height: Int)

/** Пересчёт размеров кадра под ограничение короткой стороны. */
internal fun interface VideoFrameSizing {
    /**
     * @param shortSideLimit 1080 — это «1080p» и для вертикали,
     *   и для горизонтали. `null` — размер кадра не трогаем.
     */
    fun size(width: Int, height: Int, shortSideLimit: Int?): VideoFrameSize
}

/**
 * Арифметика переехала с iOS как есть: кадр уменьшается по короткой стороне
 * и только вниз — растянуть 720p до 1080p значило бы вырастить вес на пустом
 * месте, картинке от этого лучше не станет.
 */
internal class VideoFrameSizer : VideoFrameSizing {
    override fun size(width: Int, height: Int, shortSideLimit: Int?): VideoFrameSize {
        val scale = scale(shortSide = minOf(width, height), limit = shortSideLimit)
        return VideoFrameSize(width = even(width * scale), height = even(height * scale))
    }

    private fun scale(shortSide: Int, limit: Int?): Double {
        if (limit == null || limit <= 0 || shortSide <= limit) return 1.0
        return limit.toDouble() / shortSide
    }

    /** Кодировщики требуют чётных размеров: 4:2:0 делит кадр на пары пикселей. */
    private fun even(value: Double): Int = maxOf(2, (value / 2).roundToInt() * 2)
}
