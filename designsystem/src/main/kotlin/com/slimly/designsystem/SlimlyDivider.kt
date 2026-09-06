package com.slimly.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Волосяная линия между колонками показателей внутри карточки.
 *
 * Не `VerticalDivider` из Material: тот тянется на всю высоту строки, а здесь
 * линия короче колонки — она разделяет цифры, а не режет карточку насквозь.
 */
@Composable
fun SlimlyColumnDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(vertical = SlimlySpacing.X1)
            .width(HAIRLINE)
            .height(COLUMN_HEIGHT)
            .background(SlimlyColor.Text.copy(alpha = LINE_ALPHA)),
    )
}

/** Горизонтальная линия внутри карточки — та же волосяная, но поперёк. */
@Composable
fun SlimlyCardDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HAIRLINE)
            .background(SlimlyColor.Text.copy(alpha = LINE_ALPHA)),
    )
}

private val HAIRLINE = 1.dp
private val COLUMN_HEIGHT = 34.dp
private const val LINE_ALPHA = 0.09f
