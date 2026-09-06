package com.slimly.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Кнопка Nocturne: акцентный контур на прозрачном, никогда не сплошная
 * заливка. Акцент здесь работает линией, а нажатие проявляется усилением
 * тинта.
 */
@Composable
fun SlimlyButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: SlimlyButtonEmphasis = SlimlyButtonEmphasis.TINTED,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val haptics = rememberSlimlyHaptics()
    val tint by animateColorAsState(
        targetValue = tint(emphasis, isPressed),
        label = "button-tint",
    )
    val shape = RoundedCornerShape(SlimlyRadius.Md)

    // Отдача на нажатие, а не на отпускание: палец должен услышать, что
    // кнопка его приняла, ещё до того, как что-то произойдёт.
    LaunchedEffect(isPressed) { if (isPressed) haptics(SlimlyHaptic.TAP) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(tint, shape)
            .border(width = 1.dp, color = SlimlyColor.Accent, shape = shape)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = SlimlySpacing.X4),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title, style = SlimlyText.Button, color = SlimlyColor.Accent200)
    }
}

/** Текстовое действие без рамки: «Отменить», «Вся галерея». */
@Composable
fun SlimlyLinkButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = SlimlyColor.Accent300,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val haptics = rememberSlimlyHaptics()

    LaunchedEffect(isPressed) { if (isPressed) haptics(SlimlyHaptic.TAP) }

    Text(
        text = title,
        style = SlimlyText.Body,
        // Выключенное действие гаснет, а не пропадает: на служебных экранах
        // по нему видно, что кнопка есть и появится, когда будет что делать.
        color = if (enabled) color else SlimlyColor.Neutral600,
        modifier = modifier
            .alpha(if (isPressed) PRESSED_ALPHA else 1f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(vertical = SlimlySpacing.X2),
    )
}

private fun tint(emphasis: SlimlyButtonEmphasis, isPressed: Boolean): Color = when {
    emphasis == SlimlyButtonEmphasis.TINTED && isPressed -> SlimlyColor.Accent.copy(alpha = 0.22f)
    emphasis == SlimlyButtonEmphasis.TINTED -> SlimlyColor.Accent.copy(alpha = 0.12f)
    isPressed -> SlimlyColor.Accent.copy(alpha = 0.14f)
    else -> Color.Transparent
}

private const val DISABLED_ALPHA = 0.4f
private const val PRESSED_ALPHA = 0.6f
