package com.slimly.mediakit.camera

import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.QuadPoint
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Кадр анализа лежит так, как его отдал сенсор, а рамку рисуют поверх
 * превью: если разворот углов ошибётся, рамка окажется зеркальной —
 * на экране это видно сразу, а в тесте дешевле.
 */
class CameraFrameRotationTest {
    @Test
    fun `нулевой поворот кадр не меняет`() {
        assertEquals(corner, corner.turned(0))
    }

    @Test
    fun `четверть по часовой стрелке переводит левый верхний угол в правый верхний`() {
        val turned = corner.turned(90)

        assertEquals(QuadPoint(x = 1 - 0.1f, y = 0.2f), turned.topRight)
    }

    @Test
    fun `половина оборота переворачивает кадр`() {
        val turned = corner.turned(180)

        assertEquals(QuadPoint(x = 1 - 0.2f, y = 1 - 0.1f), turned.bottomRight)
    }

    @Test
    fun `четыре четверти возвращают кадр на место`() {
        assertEquals(corner, corner.turned(360))
    }

    /** Угловые точки нарочно разные: симметричная рамка ошибку бы скрыла. */
    private val corner = DocumentQuad(
        topLeft = QuadPoint(0.2f, 0.1f),
        topRight = QuadPoint(0.8f, 0.15f),
        bottomRight = QuadPoint(0.75f, 0.9f),
        bottomLeft = QuadPoint(0.25f, 0.85f),
    )
}
