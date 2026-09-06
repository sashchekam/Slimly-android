package com.slimly.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

/**
 * Нажатие без ряби.
 *
 * Рябь Material в Nocturne чужеродна: система тёмная и отвечает на нажатие
 * изменением тинта, а не расходящимся кругом. Модификатор нужен затем, чтобы
 * это решение не переписывалось в каждом месте нажатия.
 */
fun Modifier.slimlyTap(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(interactionSource = null, indication = null, enabled = enabled, onClick = onClick)
