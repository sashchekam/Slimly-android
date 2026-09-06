package com.slimly.designsystem

import androidx.compose.ui.unit.dp

/**
 * Шкала отступов Nocturne. Плотность 0.7× уже заложена в значения — поэтому
 * числа дробные и своих «круглых» рядом заводить не нужно.
 *
 * Точка на iOS и независимый пиксель на Android — это одна и та же единица
 * плотности, поэтому числа переезжают как есть.
 */
object SlimlySpacing {
    val X1 = 2.8.dp
    val X2 = 5.6.dp
    val X3 = 8.4.dp
    val X4 = 11.2.dp
    val X6 = 16.8.dp
    val X8 = 22.4.dp

    /** Поля экрана внутри устройства — фиксированы макетом, а не шкалой. */
    val ScreenHorizontal = 20.dp
}

object SlimlyRadius {
    val Sm = 4.dp
    val Md = 8.dp
    val Lg = 14.dp

    /** Плитки галереи и мелкие превью. */
    val Tile = 10.dp
    val Thumbnail = 8.dp

    /** Верхние углы нижней шторки. */
    val Sheet = 22.dp

    /** Полностью скруглённые элементы: тумблеры, пилюли, прогресс-полосы. */
    val Pill = 999.dp
}

/** Размеры элементов, которые макет задаёт точно. */
object SlimlySize {
    val ProgressRing = 148.dp
    val ProgressRingWidth = 11.dp
    val ToggleWidth = 44.dp
    val ToggleHeight = 26.dp
}
