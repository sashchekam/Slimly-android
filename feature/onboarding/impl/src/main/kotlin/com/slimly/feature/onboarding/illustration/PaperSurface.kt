package com.slimly.feature.onboarding.illustration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.slimly.designsystem.SlimlyColor

/**
 * Лист бумаги в иллюстрациях тура: конвертированный кадр и страница скана.
 *
 * Единственная светлая поверхность во всей системе, поэтому она не белая:
 * свет идёт сверху вниз с лёгким сносом вправо, иначе прямоугольник выглядит
 * вырезанной дырой в тёмном экране.
 */
@Composable
internal fun PaperSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit = {}) {
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = listOf(SlimlyColor.PaperLight, SlimlyColor.PaperDark),
                start = Offset.Zero,
                end = Offset.Infinite,
            ),
        ),
    ) {
        content()
    }
}
