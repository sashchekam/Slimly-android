package com.slimly.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Общая обвязка экрана: фон Nocturne, поля и шапка.
 *
 * Системной панели у нас нет нигде — макет рисует и заголовок, и возврат
 * сам: заголовок живёт в контенте и уезжает со скроллом, а в шапке остаётся
 * только навигация.
 */
@Composable
fun SlimlyScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
    bottomAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        SlimlyScreenBackground()

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            SlimlyNavigationBar(onBack = onBack, trailing = trailing)
            content()
        }

        if (bottomAction != null) {
            SlimlyBottomAction(modifier = Modifier.align(Alignment.BottomCenter), content = bottomAction)
        }
    }
}

/**
 * Шапка экрана: шеврон «назад» слева, место под действие справа.
 *
 * Названия предыдущего экрана рядом с шевроном нет — оно дублирует
 * заголовок, к которому ведёт.
 */
@Composable
fun SlimlyNavigationBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TAP_TARGET)
            .padding(horizontal = SlimlySpacing.X4, vertical = SlimlySpacing.X2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(TAP_TARGET)
                    .slimlyTap(onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                // Сам шеврон крошечный: без явной области нажатия
                // в него трудно попасть пальцем.
                SlimlyChevronLeft()
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = trailing,
        )
    }
}

/**
 * Главное действие, приклеенное к низу экрана.
 *
 * Над кнопкой — растушёвка в цвет фона: контент, уезжающий под панель,
 * гаснет вместо того, чтобы обрезаться по краю. Под самой кнопкой фон
 * уже сплошной, иначе сквозь неё просвечивает список.
 */
@Composable
fun SlimlyBottomAction(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(FADE_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        listOf(SlimlyColor.Background.copy(alpha = 0f), SlimlyColor.Background),
                    ),
                ),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .background(SlimlyColor.Background)
                .navigationBarsPadding()
                .padding(horizontal = SlimlySpacing.ScreenHorizontal, vertical = SlimlySpacing.X3),
        ) {
            content()
        }
    }
}

private val TAP_TARGET = 44.dp
private val FADE_HEIGHT = 20.dp
