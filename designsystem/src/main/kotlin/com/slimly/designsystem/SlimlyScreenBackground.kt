package com.slimly.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/**
 * Фон экрана: ровная тёмная основа и мягкое пятно от левого верхнего угла.
 * Плоская заливка выглядела бы мертво, а градиент даёт глубину, не поднимая
 * насыщенность.
 *
 * Размеры пятна — те же, что на iOS: эллипс 629 × 1363 с центром у левого
 * края под вырезом, свет гаснет к середине экрана. Круг растягивается
 * в эллипс масштабом по горизонтали: своего эллиптического градиента
 * в Compose нет.
 */
@Composable
fun SlimlyScreenBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = SlimlyColor.Background)

        val width = glowWidth.toPx()
        val height = glowHeight.toPx()
        val center = Offset(x = glowCenterX.toPx(), y = 0f)
        val radius = height / 2f

        val glow = Brush.radialGradient(
            0f to SlimlyColor.BackgroundGlow,
            GLOW_FADE_STOP to SlimlyColor.BackgroundGlow.copy(alpha = 0f),
            center = center,
            radius = radius,
        )

        scale(scaleX = width / height, scaleY = 1f, pivot = center) {
            drawCircle(brush = glow, radius = radius, center = center)
        }
    }
}

private val glowWidth = 629.dp
private val glowHeight = 1363.dp
private val glowCenterX = 31.dp

/** Доля радиуса, на которой свет гаснет полностью. */
private const val GLOW_FADE_STOP = 0.62f
