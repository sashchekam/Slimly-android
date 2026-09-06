package com.slimly.mediakit.camera

import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.QuadPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoShutterTest {
    private val shutter = AutoShutter()

    @Test
    fun `без листа в кадре не снимаем никогда`() {
        assertFalse(shutter.shouldShoot(quad = null, at = 0))
        assertFalse(shutter.shouldShoot(quad = null, at = SECOND * 10))
    }

    @Test
    fun `первый кадр с листом ещё не повод снимать`() {
        assertFalse(shutter.shouldShoot(quad = sheet(), at = 0))
    }

    @Test
    fun `секунда неподвижности спускает затвор`() {
        shutter.shouldShoot(sheet(), at = 0)
        assertFalse(shutter.shouldShoot(sheet(), at = HALF_SECOND))
        assertTrue(shutter.shouldShoot(sheet(), at = SECOND))
    }

    @Test
    fun `сдвиг листа начинает отсчёт заново`() {
        shutter.shouldShoot(sheet(), at = 0)
        shutter.shouldShoot(sheet(offset = 0.2f), at = HALF_SECOND)

        assertFalse(shutter.shouldShoot(sheet(offset = 0.2f), at = SECOND))
        assertTrue(shutter.shouldShoot(sheet(offset = 0.2f), at = SECOND + HALF_SECOND))
    }

    @Test
    fun `дрожание руки неподвижности не отменяет`() {
        shutter.shouldShoot(sheet(), at = 0)
        assertTrue(shutter.shouldShoot(sheet(offset = 0.001f), at = SECOND))
    }

    @Test
    fun `после снимка отсчёт идёт с начала`() {
        shutter.shouldShoot(sheet(), at = 0)
        shutter.shouldShoot(sheet(), at = SECOND)

        assertFalse(shutter.shouldShoot(sheet(), at = SECOND + 1))
    }

    private fun sheet(offset: Float = 0f) = DocumentQuad(
        topLeft = QuadPoint(0.1f + offset, 0.1f),
        topRight = QuadPoint(0.9f + offset, 0.1f),
        bottomRight = QuadPoint(0.9f + offset, 0.9f),
        bottomLeft = QuadPoint(0.1f + offset, 0.9f),
    )

    private companion object {
        const val HALF_SECOND = 500L
        const val SECOND = 1000L
    }
}
