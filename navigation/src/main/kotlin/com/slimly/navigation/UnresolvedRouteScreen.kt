package com.slimly.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText

/**
 * Заглушка на случай маршрута, для которого забыли зарегистрировать резолвер.
 *
 * Экран рисует сама навигация — это единственное её место с вёрсткой:
 * показать нерешённый маршрут больше некому, а выглядеть он должен как часть
 * приложения, а не как системная ошибка.
 */
@Composable
internal fun UnresolvedRouteScreen(route: Route, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlimlyColor.Background)
            .padding(horizontal = SlimlySpacing.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.unresolved_title),
            style = SlimlyText.Title,
            color = SlimlyColor.Text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.unresolved_description, route.toString()),
            style = SlimlyText.Body,
            color = SlimlyColor.Neutral500,
            textAlign = TextAlign.Center,
        )
    }
}
