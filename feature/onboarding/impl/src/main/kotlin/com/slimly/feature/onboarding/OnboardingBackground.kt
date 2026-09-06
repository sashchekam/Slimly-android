package com.slimly.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import com.slimly.designsystem.SlimlyColor

/**
 * Фон вводного тура: свет собран сверху по центру, а не у левого угла,
 * как на рабочих экранах.
 *
 * Здесь нет ни списка, ни шапки — смотрят в иллюстрацию, и пятно стоит прямо
 * над ней. К низу фон уходит темнее основного: кнопка внизу должна лежать
 * на самом глухом участке.
 */
@Composable
internal fun OnboardingBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(SlimlyColor.TourEdge)
            .drawBehind {
                // Радиальный градиент на Android круглый, а нужен эллипс шире
                // экрана: рисуем круг и растягиваем кадр по горизонтали.
                val radius = size.width * GLOW_RADIUS
                scale(scaleX = GLOW_STRETCH, scaleY = 1f, pivot = Offset(size.width / 2f, size.height * GLOW_TOP)) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to SlimlyColor.TourGlow,
                                GLOW_FALLOFF to SlimlyColor.Background,
                                1f to SlimlyColor.TourEdge,
                            ),
                            center = Offset(size.width / 2f, size.height * GLOW_TOP),
                            radius = radius,
                        ),
                        size = Size(size.width, size.height),
                    )
                }
            },
    )
}

/** Пятно шире экрана: свет гаснет примерно там, где начинается текст. */
private const val GLOW_STRETCH = 2.4f
private const val GLOW_RADIUS = 0.62f
private const val GLOW_TOP = 0.04f
private const val GLOW_FALLOFF = 0.58f
