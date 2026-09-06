package com.slimly.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Подтверждение действия, у которого нет видимых последствий: «скопировано»,
 * «сброшено», «ещё два раза — и откроется отладочное меню».
 *
 * Не диалог: сообщение ничего не спрашивает и уходит само, поэтому у него нет
 * ни кнопки, ни области нажатия — палец оно не перехватывает.
 */
@Composable
fun SlimlyToast(text: String, modifier: Modifier = Modifier) {
    // Скругление «до упора»: на одну строку это капсула, на две — стадион.
    // Прямые углы у всплывающего сообщения делают его похожим на карточку
    // контента, которая никуда не денется.
    val shape = RoundedCornerShape(SlimlyRadius.Pill)

    Row(
        modifier = modifier
            .background(SlimlyColor.Menu, shape)
            .border(
                width = EDGE,
                // Верхняя кромка светлее нижней: стекло ловит свет сверху,
                // как весь остальной Nocturne, и не выглядит наклейкой.
                brush = Brush.verticalGradient(
                    listOf(
                        SlimlyColor.Neutral100.copy(alpha = EDGE_TOP_ALPHA),
                        SlimlyColor.Neutral100.copy(alpha = EDGE_BOTTOM_ALPHA),
                    ),
                ),
                shape = shape,
            )
            .padding(horizontal = HORIZONTAL, vertical = VERTICAL),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Точка вместо галочки: она отмечает начало строки, но ничего
        // не обещает, — а галочка рядом с «не удалось» сказала бы неправду.
        Box(Modifier.size(DOT).background(SlimlyColor.Accent, CircleShape))

        Text(text = text, style = SlimlyText.BodySmall, color = SlimlyColor.Neutral100)
    }
}

/**
 * Тост поверх экрана. Его жизнь принадлежит состоянию: вьюмодель ставит текст
 * и она же обнуляет — экран своих таймеров не заводит.
 */
@Composable
fun BoxScope.SlimlyToastHost(text: String?, modifier: Modifier = Modifier) {
    // Текст переживает своё обнуление: он гаснет раньше, чем доигрывает
    // анимация ухода, и сообщение мигнуло бы пустой капсулой.
    var lastText by remember { mutableStateOf("") }
    if (text != null) lastText = text

    AnimatedVisibility(
        visible = text != null,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(horizontal = SlimlySpacing.ScreenHorizontal, vertical = BOTTOM),
        // Подъём снизу с лёгким «раскрытием»: сообщение растёт из-под края
        // экрана, а не появляется из воздуха.
        enter = slideInVertically(spring()) { it } + fadeIn() + scaleIn(initialScale = SCALE_FROM),
        exit = slideOutVertically(spring()) { it } + fadeOut() + scaleOut(targetScale = SCALE_FROM),
    ) {
        SlimlyToast(lastText)
    }
}

private val EDGE = 1.dp
private val HORIZONTAL = 15.dp
private val VERTICAL = 12.dp
private val DOT = 6.dp
private val BOTTOM = 22.dp
private const val EDGE_TOP_ALPHA = 0.16f
private const val EDGE_BOTTOM_ALPHA = 0.04f
private const val SCALE_FROM = 0.94f
