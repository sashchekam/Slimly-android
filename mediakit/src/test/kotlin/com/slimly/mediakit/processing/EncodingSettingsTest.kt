package com.slimly.mediakit.processing

import com.slimly.core.model.CompressionLevel
import com.slimly.core.model.CompressionPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodingSettingsTest {
    private val settings = EncodingSettings()

    @Test
    fun `слабое сжатие держит качество и полный кадр`() {
        val level = CompressionLevel.at(CompressionPreset.LIGHT)

        assertEquals(85, settings.imageQuality(level))
        assertEquals(4032, settings.imageMaxPixelSize(level))
        assertEquals(256, settings.imagePaletteColors(level))
    }

    @Test
    fun `сильное сжатие уходит в нижний край шкалы`() {
        val level = CompressionLevel.at(CompressionPreset.MAXIMUM)

        assertEquals(35, settings.imageQuality(level))
        assertEquals(1600, settings.imageMaxPixelSize(level))
        assertEquals(64, settings.imagePaletteColors(level))
    }

    @Test
    fun `середина шкалы лежит между краями`() {
        val level = CompressionLevel.at(CompressionPreset.MEDIUM)

        assertTrue(settings.imageQuality(level) in 36..84)
        assertTrue(settings.imageMaxPixelSize(level) in 1601..4031)
        assertTrue(settings.imagePaletteColors(level) in 65..255)
    }
}
