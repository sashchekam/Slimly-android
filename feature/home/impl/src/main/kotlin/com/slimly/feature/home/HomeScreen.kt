package com.slimly.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap
import com.slimly.feature.home.api.HomeTool

/**
 * Хаб инструментов: приветствие, имя приложения и карточки.
 *
 * Карточки приходят готовым списком — главная не знает ни одной чужой фичи
 * и открывает то, что ей дали.
 */
@Composable
internal fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    SlimlyScreen(
        modifier = modifier,
        trailing = { ProfileButton(onClick = viewModel::openProfile) },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = SlimlySpacing.ScreenHorizontal,
                end = SlimlySpacing.ScreenHorizontal,
                bottom = SlimlySpacing.X8,
            ),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        ) {
            item {
                Column(Modifier.padding(bottom = SlimlySpacing.X4)) {
                    Text(
                        text = stringResource(greetingRes(viewModel.greeting)).uppercase(),
                        style = SlimlyText.Kicker,
                        color = SlimlyColor.Neutral500,
                    )
                    Text(
                        text = stringResource(R.string.home_title),
                        style = SlimlyText.LargeTitle,
                        color = SlimlyColor.Text,
                        modifier = Modifier.padding(top = SlimlySpacing.X2),
                    )
                }
            }

            items(items = viewModel.tools, key = { it.id }) { tool ->
                ToolCard(tool = tool, onClick = { viewModel.open(tool) })
            }
        }
    }
}

@Composable
private fun ToolCard(tool: HomeTool, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlimlyColor.Surface, RoundedCornerShape(SlimlyRadius.Lg))
            .slimlyTap(onClick = onClick)
            .padding(SlimlySpacing.X6),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X1),
    ) {
        Text(text = tool.title, style = SlimlyText.SectionTitle, color = SlimlyColor.Text)
        Text(text = tool.subtitle, style = SlimlyText.Body, color = SlimlyColor.Neutral500)
    }
}

/** Круглая кнопка в шапке — вход в профиль. */
@Composable
private fun ProfileButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(BUTTON_SIZE)
            .background(SlimlyColor.Surface, CircleShape)
            .slimlyTap(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.home_profile), style = SlimlyText.Nano, color = SlimlyColor.Neutral300)
    }
}

private fun greetingRes(greeting: Greeting): Int = when (greeting) {
    Greeting.MORNING -> R.string.greeting_morning
    Greeting.AFTERNOON -> R.string.greeting_afternoon
    Greeting.EVENING -> R.string.greeting_evening
    Greeting.NIGHT -> R.string.greeting_night
}

private val BUTTON_SIZE = 40.dp
