package com.slimly.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Отклик на палец.
 *
 * Одним местом на всё приложение: сила отдачи — решение системы, а не экрана,
 * и разнобой в ней слышен так же, как разнобой в цвете виден. Поэтому здесь
 * не голые константы платформы, а три случая, ради которых отдача вообще
 * заводится.
 */
enum class SlimlyHaptic(internal val constant: Int) {
    /** Нажатие на кнопку — самый лёгкий отклик. */
    TAP(HapticFeedbackConstants.KEYBOARD_TAP),

    /** Что-то произошло само: страница снята, кадр пойман. */
    IMPACT(HapticFeedbackConstants.LONG_PRESS),

    /** Взялись за элемент или отпустили его. */
    SELECTION(HapticFeedbackConstants.CLOCK_TICK),
}

/**
 * Отдача, привязанная к текущему окну.
 *
 * Через `View`, а не через `LocalHapticFeedback`: набор случаев у Compose
 * свой и с системными привычками совпадает не полностью, а заставка
 * приложения уже говорит с системой напрямую — двух разных способов
 * тут быть не должно.
 */
@Composable
fun rememberSlimlyHaptics(): (SlimlyHaptic) -> Unit {
    val view = LocalView.current
    return remember(view) { { haptic -> view.performHapticFeedback(haptic.constant) } }
}
