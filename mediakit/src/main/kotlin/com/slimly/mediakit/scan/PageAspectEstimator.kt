package com.slimly.mediakit.scan

import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.QuadPoint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/** Истинные пропорции листа по его проекции в кадре. */
internal fun interface PageAspectEstimating {
    /**
     * Отношение ширины листа к его высоте или `null`, если по этому
     * четырёхугольнику восстановить его нельзя.
     *
     * Кадр приходит парой чисел, а не `Size`: здесь чистая арифметика над
     * долями, и системный тип ей ни к чему — зато без него расчёт
     * проверяется тестом без устройства.
     */
    fun ratio(quad: DocumentQuad, width: Int, height: Int): Float?
}

/**
 * Метод Чжана и Хэ: у прямоугольника в перспективе две пары параллельных
 * сторон, и по точкам схода этих пар восстанавливаются сразу и фокусное
 * расстояние камеры, и отношение сторон самого прямоугольника.
 *
 * Без этого шага страница выходит сплющенной. Выпрямление снимает наклон,
 * но размер готовой картинки берёт по длинам сторон четырёхугольника как они
 * есть, а дальний край листа в кадре короче ближнего — и лист A4 приезжает
 * в PDF то узким, то широким, смотря с какого ракурса его сняли.
 *
 * Ссылка: Z. Zhang, L. He. Whiteboard scanning and image enhancement.
 * Digital Signal Processing, 2007.
 */
internal class PageAspectEstimator : PageAspectEstimating {
    override fun ratio(quad: DocumentQuad, width: Int, height: Int): Float? {
        if (width <= 0 || height <= 0) return null

        // Начало координат — в центре кадра: формулы ниже считают углы
        // лучами из оптического центра, а не точками растра.
        fun centred(point: QuadPoint) = Ray(
            x = (point.x * width - width / 2f).toDouble(),
            y = (point.y * height - height / 2f).toDouble(),
        )

        val corners = Corners(
            topLeft = centred(quad.topLeft),
            topRight = centred(quad.topRight),
            bottomLeft = centred(quad.bottomLeft),
            bottomRight = centred(quad.bottomRight),
        )

        val ratio = corners.ratio()?.toFloat() ?: return null
        return ratio.takeIf { it in SANE_LOW..SANE_HIGH }
    }

    private companion object {
        /**
         * Оценка вне этих пределов считается вырожденной: столько не бывает
         * даже у кассового чека, а значит, углы пришли неверные.
         */
        const val SANE_LOW = 0.05f
        const val SANE_HIGH = 20f
    }
}

/** Угол листа лучом из оптического центра. */
private data class Ray(val x: Double, val y: Double)

/**
 * Углы листа лучами из оптического центра. Отдельный тип, потому что весь
 * расчёт — про их взаимное расположение и больше ни про что.
 */
private class Corners(val topLeft: Ray, val topRight: Ray, val bottomLeft: Ray, val bottomRight: Ray) {
    fun ratio(): Double? {
        val horizontal = converge(topRight, topLeft, bottomLeft) ?: return null
        val vertical = converge(bottomLeft, topLeft, topRight) ?: return null

        // Стороны параллельны — перспективы нет, и отношение сторон
        // читается прямо с картинки.
        val hasPerspective = abs(horizontal - 1) > TOLERANCE && abs(vertical - 1) > TOLERANCE
        return (if (hasPerspective) projected(horizontal, vertical) else null) ?: flat()
    }

    /**
     * Отношение сторон через точки схода. `null` — такой камеры не бывает:
     * углы описывают не прямоугольник, а произвольный четырёхугольник,
     * и восстанавливать по ним нечего.
     */
    private fun projected(horizontal: Double, vertical: Double): Double? {
        val width = scaled(topRight, horizontal)
        val height = scaled(bottomLeft, vertical)

        // Квадрат фокусного расстояния в пикселях кадра.
        val focal = -(height.y * width.y + height.x * width.x) / ((vertical - 1) * (horizontal - 1))
        if (focal <= 0) return null

        val across = (horizontal - 1) * (horizontal - 1) + (width.x * width.x + width.y * width.y) / focal
        val along = (vertical - 1) * (vertical - 1) + (height.x * height.x + height.y * height.y) / focal
        if (along <= 0) return null

        return sqrt(across / along)
    }

    /**
     * Коэффициент схода стороны: во сколько раз её дальний конец «сжат»
     * перспективой относительно ближнего.
     */
    private fun converge(corner: Ray, origin: Ray, other: Ray): Double? {
        val divisor = cross(corner, other)
        if (abs(divisor) <= ZERO) return null
        return cross(origin, other) / divisor
    }

    /**
     * Смешанное произведение трёх лучей: ноль означает, что они лежат
     * в одной плоскости, то есть точка схода этой пары сторон найдена.
     */
    private fun cross(point: Ray, other: Ray): Double =
        (point.y - bottomRight.y) * other.x - (point.x - bottomRight.x) * other.y +
            point.x * bottomRight.y - point.y * bottomRight.x

    private fun scaled(corner: Ray, factor: Double) =
        Ray(x = factor * corner.x - topLeft.x, y = factor * corner.y - topLeft.y)

    /** Отношение сторон без перспективы — просто их длины. */
    private fun flat(): Double? {
        val across = hypot(topRight.x - topLeft.x, topRight.y - topLeft.y)
        val along = hypot(bottomLeft.x - topLeft.x, bottomLeft.y - topLeft.y)
        return if (along > 0) across / along else null
    }

    private companion object {
        /**
         * Насколько коэффициенты схода должны отличаться от единицы, чтобы
         * перспектива считалась заметной. Ровно единица означает параллельные
         * стороны: точка схода уходит в бесконечность, и общая формула
         * делится на ноль там, где ответ и так известен.
         */
        const val TOLERANCE = 1e-6
        const val ZERO = 1e-9
    }
}
