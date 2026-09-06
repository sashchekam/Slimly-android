package com.slimly.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * Сетка знака: квадраты 30×30 в поле 100×100, центры разведены на 21
 * от середины, всё вместе повёрнуто на 45°.
 *
 * Одна на всех: статичный знак, анимацию заставки и растр адаптивной иконки.
 * Правишь сетку — перерисовывай иконку: разъехавшиеся пропорции видно сразу,
 * потому что иконка и заставка идут друг за другом при старте.
 */
object SlimlyMarkGrid {
    const val BOX = 100f
    const val CELL = 30f
    const val CORNER = 8f
    const val OFFSET = 21f

    /** Толщина контура в единицах сетки. */
    const val STROKE = 5.5f

    /**
     * Залитый квадрат в анимации крупнее обведённых: он занимает их внешние
     * габариты (30 + 5.5 контура), иначе на ходу пятно «худеет» рядом
     * с контурами, мимо которых летит.
     */
    const val ACCENT_CELL = 36f
    const val ACCENT_CORNER = 10f

    /**
     * Углы обхода в координатах сетки. После поворота на 45° это, по экрану,
     * низ → лево → верх → право — то есть против часовой стрелки.
     */
    val CORNERS = listOf(
        Offset(OFFSET, OFFSET),
        Offset(-OFFSET, OFFSET),
        Offset(-OFFSET, -OFFSET),
        Offset(OFFSET, -OFFSET),
    )

    /** Контур, который в покое горит белым, — верхний на экране. */
    const val LIT_CORNER = 2

    /** Поворот сетки: ромб, а не квадрат. */
    const val ROTATION = 45f

    /**
     * Толщина контура на экране. Пропорция макета — 5.5 % стороны, но тоньше
     * 2.8 dp контур на тёмном фоне перестаёт читаться, поэтому подпёрт снизу.
     */
    fun DrawScope.markLineWidth(): Float = maxOf(size.width * STROKE / BOX, MIN_STROKE.toPx())

    private val MIN_STROKE = 2.8.dp
}
