package com.slimly.mediakit.camera

import androidx.camera.core.ImageProxy
import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.QuadPoint
import com.slimly.core.service.DocumentBoundsDetector
import com.slimly.mediakit.scan.ContourBoundsDetector

/** Живой кадр так, как его видит человек: пропорции и найденный в нём лист. */
internal data class CameraFrame(val quad: DocumentQuad?, val aspectRatio: Float)

/** Разбор кадра видоискателя. */
internal fun interface CameraFrameReading {
    fun read(image: ImageProxy): CameraFrame
}

/**
 * Кадр анализа приходит так, как его отдал сенсор, — то есть повёрнутым
 * относительно того, что показывает превью. Рамку рисуют поверх превью,
 * поэтому кадр здесь и разворачивается: один раз на границе с камерой,
 * а не в каждом месте отрисовки.
 *
 * Разворачиваем не картинку, а четыре точки: поворот растра 640 × 480
 * шесть раз в секунду — это работа ради того же результата.
 */
internal class CameraFrameReader(private val detector: DocumentBoundsDetector = ContourBoundsDetector()) :
    CameraFrameReading {
    override fun read(image: ImageProxy): CameraFrame {
        val degrees = image.imageInfo.rotationDegrees
        val frame = image.toBitmap()
        val quad = try {
            detector.bounds(frame)
        } finally {
            // Копия кадра наша: поиск листа работает на своей уменьшенной,
            // а эту за нами никто не уберёт.
            frame.recycle()
        }

        val isSideways = degrees == QUARTER || degrees == QUARTER * 3
        val width = if (isSideways) image.height else image.width
        val height = if (isSideways) image.width else image.height

        return CameraFrame(
            quad = quad?.turned(degrees),
            aspectRatio = if (height > 0) width.toFloat() / height else DEFAULT_ASPECT_RATIO,
        )
    }
}

/**
 * Тот же лист в координатах повёрнутого кадра.
 *
 * Имена углов едут вместе с картинкой: после поворота на четверть по часовой
 * стрелке левый верхний угол листа оказывается правым верхним — и рамка,
 * оставленная в прежнем порядке, сложилась бы бантиком.
 */
internal fun DocumentQuad.turned(degrees: Int): DocumentQuad {
    val steps = ((degrees / QUARTER) % TURNS + TURNS) % TURNS
    if (steps == 0) return this

    val moved = corners.map { it.turned(steps) }
    val ordered = List(TURNS) { index -> moved[(index - steps + TURNS) % TURNS] }
    return DocumentQuad(
        topLeft = ordered[0],
        topRight = ordered[1],
        bottomRight = ordered[2],
        bottomLeft = ordered[3],
    )
}

private fun QuadPoint.turned(steps: Int): QuadPoint = when (steps) {
    1 -> QuadPoint(x = 1 - y, y = x)
    2 -> QuadPoint(x = 1 - x, y = 1 - y)
    else -> QuadPoint(x = y, y = 1 - x)
}

private const val QUARTER = 90
private const val TURNS = 4

/** Пропорции кадра, если камера ещё не сказала своих: обычный портретный. */
private const val DEFAULT_ASPECT_RATIO = 3f / 4f
