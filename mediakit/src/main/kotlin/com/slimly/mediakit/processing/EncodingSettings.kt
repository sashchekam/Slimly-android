package com.slimly.mediakit.processing

import com.slimly.core.model.CompressionLevel
import kotlin.math.roundToInt

/**
 * Как степень сжатия превращается в настройки кодировщиков.
 *
 * Ползунок пользователь понимает как «сильнее / слабее», а кодировщикам нужны
 * качество, разрешение и битрейт — перевод живёт здесь, рядом с кодированием,
 * а не в модели.
 */
internal interface CompressionSettingsProviding {
    fun imageQuality(level: CompressionLevel): Int

    fun imageMaxPixelSize(level: CompressionLevel): Int

    fun imagePaletteColors(level: CompressionLevel): Int

    fun videoShortSide(level: CompressionLevel): Int

    fun videoBitrateFactor(level: CompressionLevel): Double
}

internal class EncodingSettings : CompressionSettingsProviding {
    /**
     * Качество JPEG. Ниже 35 артефакты видны на глаз даже в мессенджере,
     * поэтому шкала там и заканчивается.
     */
    override fun imageQuality(level: CompressionLevel): Int =
        interpolate(level.value, from = QUALITY_HIGH, to = QUALITY_LOW).roundToInt()

    /**
     * Длинная сторона кадра. Верх шкалы — примерно 12 Мп, низ — 2 Мп: именно
     * уменьшение кадра, а не качество, даёт основной выигрыш в весе.
     */
    override fun imageMaxPixelSize(level: CompressionLevel): Int =
        interpolate(level.value, from = SIDE_LONG, to = SIDE_SHORT).roundToInt()

    /**
     * Длина палитры PNG. У формата без потерь качество не работает, и вес
     * задаёт именно она: 256 цветов плоская графика переживает без единого
     * изменения, а 64 заметны только на плавных переходах.
     */
    override fun imagePaletteColors(level: CompressionLevel): Int =
        interpolate(level.value, from = PALETTE_FULL, to = PALETTE_TIGHT).roundToInt()

    /**
     * Короткая сторона видео: до середины шкалы держим 1080p, дальше уходим
     * в 720p — ровно то, что обещают подписи пресетов.
     */
    override fun videoShortSide(level: CompressionLevel): Int = if (level.value < SIDE_STEP) SIDE_FULL_HD else SIDE_HD

    /**
     * Доля исходного битрейта. Вес результата задаёт именно она: разрешение
     * влияет на то, как этот битрейт выглядит, а не на объём.
     */
    override fun videoBitrateFactor(level: CompressionLevel): Double = level.ratio

    private fun interpolate(value: Double, from: Double, to: Double): Double =
        from + (to - from) * value.coerceIn(0.0, 1.0)

    private companion object {
        const val QUALITY_HIGH = 85.0
        const val QUALITY_LOW = 35.0
        const val SIDE_LONG = 4032.0
        const val SIDE_SHORT = 1600.0
        const val PALETTE_FULL = 256.0
        const val PALETTE_TIGHT = 64.0
        const val SIDE_FULL_HD = 1080
        const val SIDE_HD = 720

        /** Где шкала переходит с 1080p на 720p. */
        const val SIDE_STEP = 0.66
    }
}
