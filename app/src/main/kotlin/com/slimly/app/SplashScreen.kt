package com.slimly.app

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyLogoMarkAnimation

/**
 * Заставка при запуске: знак обходит круг, потом открывается приложение.
 *
 * Системный экран запуска уже показал первый кадр — ту же заливку и тот же
 * знак в центре, — поэтому заставка ничего не проявляет: она подхватывает
 * готовый кадр и просто оживляет его.
 */
@Composable
internal fun SplashScreen(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxSize()
            // Свет собран вокруг знака, а не у края экрана, как на рабочих
            // экранах: здесь он — единственное, на что смотрят.
            .drawBehind {
                drawRect(SlimlyColor.Background)
                drawRect(
                    Brush.radialGradient(
                        colorStops = arrayOf(0f to SlimlyColor.SplashGlow, GLOW_EDGE to SlimlyColor.Background),
                        center = Offset(size.width / 2f, size.height * GLOW_CENTER),
                        radius = size.width * GLOW_RADIUS,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        SlimlyLogoMarkAnimation(
            // 117 — сторона сетки знака в макете анимации. Крупнее статичного:
            // летящему акценту нужен запас на замах.
            size = MARK_SIZE,
            // Удары набирают силу к последнему углу: рука понимает, что обход
            // заканчивается, раньше, чем это замечает глаз.
            onLanding = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
            onFinish = {
                // Мягкий удар в момент передачи: он тяжелее ударов по углам,
                // поэтому круг заканчивается не затиханием, а точкой.
                // `LONG_PRESS`, а не `CONFIRM`: тот появился только в Android 11,
                // а нижняя граница у нас 10.
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onFinish()
            },
        )
    }
}

private val MARK_SIZE = 117.dp
private const val GLOW_CENTER = 0.32f
private const val GLOW_RADIUS = 1.2f
private const val GLOW_EDGE = 0.62f
