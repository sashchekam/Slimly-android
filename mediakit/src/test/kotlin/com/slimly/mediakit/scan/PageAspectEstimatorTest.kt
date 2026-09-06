package com.slimly.mediakit.scan

import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.QuadPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Восстановление пропорций листа по его проекции — тот самый расчёт, ошибка
 * в котором видна как «буквы растянуло». Углы задаются числами, устройство
 * для проверки не нужно.
 */
class PageAspectEstimatorTest {
    private val estimator = PageAspectEstimator()

    @Test
    fun `лист без наклона отдаёт свои же пропорции`() {
        val ratio = estimator.ratio(rectangle(left = 0.2f, top = 0.1f, right = 0.8f, bottom = 0.9f), WIDTH, HEIGHT)

        // Прямоугольник 0,6 × 0,8 доли кадра 1000 × 1000.
        assertEquals(0.75f, requireNotNull(ratio), TOLERANCE)
    }

    @Test
    fun `наклонённый лист восстанавливается ближе к своей форме, чем проекция`() {
        // Верхний край короче нижнего — лист снят под углом сверху.
        val quad = DocumentQuad(
            topLeft = QuadPoint(0.32f, 0.18f),
            topRight = QuadPoint(0.68f, 0.18f),
            bottomRight = QuadPoint(0.84f, 0.84f),
            bottomLeft = QuadPoint(0.16f, 0.84f),
        )

        val ratio = requireNotNull(estimator.ratio(quad, WIDTH, HEIGHT))

        // Проекция говорит про 0,54 ширины на 0,66 высоты — почти квадрат;
        // на столе лист заметно уже.
        assertTrue("Оценка $ratio не уже проекции", ratio < PROJECTED)
        assertTrue("Оценка $ratio вырождена", ratio > 0.2f)
    }

    @Test
    fun `вырожденный кадр оценивать нечем`() {
        val quad = rectangle(left = 0.2f, top = 0.1f, right = 0.8f, bottom = 0.9f)

        assertNull(estimator.ratio(quad, width = 0, height = HEIGHT))
        assertNull(estimator.ratio(quad, width = WIDTH, height = 0))
    }

    @Test
    fun `схлопнутый четырёхугольник отбрасывается`() {
        // Все углы в одной точке: восстанавливать нечего.
        val point = QuadPoint(0.5f, 0.5f)
        val quad = DocumentQuad(topLeft = point, topRight = point, bottomRight = point, bottomLeft = point)

        assertNull(estimator.ratio(quad, WIDTH, HEIGHT))
    }

    private fun rectangle(left: Float, top: Float, right: Float, bottom: Float) = DocumentQuad(
        topLeft = QuadPoint(left, top),
        topRight = QuadPoint(right, top),
        bottomRight = QuadPoint(right, bottom),
        bottomLeft = QuadPoint(left, bottom),
    )

    private companion object {
        const val WIDTH = 1000
        const val HEIGHT = 1000
        const val TOLERANCE = 0.01f

        /** Отношение сторон так, как их видно в кадре, без поправки. */
        const val PROJECTED = 0.54f / 0.66f
    }
}
