package com.slimly.mediakit.processing.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFrameSizerTest {
    private val sizer = VideoFrameSizer()

    @Test
    fun `кадр меньше предела остаётся как был`() {
        assertEquals(VideoFrameSize(1280, 720), sizer.size(width = 1280, height = 720, shortSideLimit = 1080))
    }

    @Test
    fun `предел применяется к короткой стороне независимо от ориентации`() {
        assertEquals(VideoFrameSize(1280, 720), sizer.size(width = 1920, height = 1080, shortSideLimit = 720))
        assertEquals(VideoFrameSize(720, 1280), sizer.size(width = 1080, height = 1920, shortSideLimit = 720))
    }

    @Test
    fun `стороны получаются чётными`() {
        val size = sizer.size(width = 1439, height = 1079, shortSideLimit = 720)

        assertEquals(0, size.width % 2)
        assertEquals(0, size.height % 2)
    }

    @Test
    fun `без предела кадр не трогаем`() {
        assertEquals(VideoFrameSize(3840, 2160), sizer.size(width = 3840, height = 2160, shortSideLimit = null))
    }
}
