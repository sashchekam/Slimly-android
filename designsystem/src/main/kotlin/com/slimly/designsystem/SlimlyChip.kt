package com.slimly.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Пункт выбора одной строкой: формат, срез времени, режим страницы.
 *
 * Выбранный чип красится тонкой акцентной заливкой и акцентной кромкой,
 * а не сплошным акцентом: в ряду из трёх пунктов сплошная заливка перетянула
 * бы на себя весь экран.
 */
@Composable
fun SlimlyChip(title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(SlimlyRadius.Md)

    Text(
        text = title,
        style = SlimlyText.ChipLabel,
        color = if (isSelected) SlimlyColor.Accent200 else SlimlyColor.Neutral300,
        // По центру, чтобы чип одинаково читался и по ширине надписи,
        // и растянутым на равные доли ряда — второе решает вызывающий,
        // отдавая чипу вес.
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .background(if (isSelected) SlimlyColor.Accent.copy(alpha = SELECTED_FILL) else SlimlyColor.Surface)
            .border(
                width = EDGE,
                color = if (isSelected) SlimlyColor.Accent else SlimlyColor.Text.copy(alpha = EDGE_ALPHA),
                shape = shape,
            )
            .slimlyTap(onClick = onClick)
            .padding(horizontal = HORIZONTAL, vertical = VERTICAL),
    )
}

private val EDGE = 1.dp
private val HORIZONTAL = 15.dp
private val VERTICAL = 10.dp
private const val SELECTED_FILL = 0.14f
private const val EDGE_ALPHA = 0.1f
