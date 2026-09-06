package com.slimly.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyCard
import com.slimly.designsystem.SlimlyChip
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyLinkButton
import com.slimly.designsystem.SlimlyRowGroup
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.SlimlyToastHost
import com.slimly.designsystem.slimlyRow
import com.slimly.designsystem.slimlyTap

/**
 * Журнал аналитики: что ушло наружу, когда и с какими параметрами.
 *
 * Экран отвечает на вопрос, на который веб-консоль отвечает через сутки:
 * ушло ли событие вообще и то ли в нём лежит.
 */
@Composable
fun DebugAnalyticsScreen(viewModel: DebugAnalyticsViewModel, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        SlimlyScreen(
            onBack = viewModel::goBack,
            trailing = { Tools(viewModel) },
        ) {
            val entries = viewModel.entries

            LazyColumn(
                contentPadding = PaddingValues(
                    start = SlimlySpacing.ScreenHorizontal,
                    end = SlimlySpacing.ScreenHorizontal,
                    bottom = BOTTOM_INSET,
                ),
                verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.menu_analytics),
                        style = SlimlyText.Title,
                        color = SlimlyColor.Text,
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.analytics_note, viewModel.capacity),
                        style = SlimlyText.Caption,
                        color = SlimlyColor.Neutral500,
                    )
                }

                if (viewModel.scopes.isNotEmpty()) {
                    item { Scopes(viewModel) }
                }

                if (entries.isEmpty()) {
                    item { EmptyCard() }
                } else {
                    item { Records(entries, viewModel) }
                }
            }
        }

        SlimlyToastHost(viewModel.toast)
    }
}

@Composable
private fun Tools(viewModel: DebugAnalyticsViewModel) {
    // Пустой журнал нечего ни копировать, ни чистить — но кнопки остаются
    // на месте: иначе шапка прыгает от первого же события.
    val enabled = !viewModel.isEmpty

    Row(horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X4)) {
        SlimlyLinkButton(
            title = stringResource(R.string.analytics_copy),
            onClick = viewModel::copyAll,
            enabled = enabled,
        )
        SlimlyLinkButton(
            title = stringResource(R.string.analytics_clear),
            onClick = viewModel::clear,
            enabled = enabled,
        )
    }
}

/**
 * Фильтр по префиксу модуля: за один флоу событий набирается несколько
 * десятков, и своё среди чужих глазами уже не выхватить.
 */
@Composable
private fun Scopes(viewModel: DebugAnalyticsViewModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
    ) {
        SlimlyChip(
            title = stringResource(R.string.analytics_all),
            isSelected = viewModel.isSelected(null),
            onClick = { viewModel.select(null) },
        )
        viewModel.scopes.forEach { scope ->
            SlimlyChip(
                title = scope,
                isSelected = viewModel.isSelected(scope),
                onClick = { viewModel.select(scope) },
            )
        }
    }
}

@Composable
private fun Records(entries: List<DebugAnalyticsEntry>, viewModel: DebugAnalyticsViewModel) {
    SlimlyRowGroup {
        entries.forEach { entry ->
            Column(
                modifier = Modifier
                    .slimlyTap(enabled = entry.isExpandable) { viewModel.toggle(entry) }
                    .slimlyRow(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.name,
                        style = SlimlyText.Mono,
                        color = SlimlyColor.Text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = entry.time, style = SlimlyText.MonoSmall, color = SlimlyColor.Neutral600)
                }

                if (viewModel.isExpanded(entry)) {
                    Parameters(entry)
                } else if (entry.summary.isNotEmpty()) {
                    Text(
                        text = entry.summary,
                        style = SlimlyText.MonoSmall,
                        color = SlimlyColor.Neutral500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = SlimlySpacing.X1),
                    )
                }
            }
        }
    }
}

@Composable
private fun Parameters(entry: DebugAnalyticsEntry) {
    Column(
        modifier = Modifier.padding(top = SlimlySpacing.X1),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X1),
    ) {
        entry.parameters.forEach { parameter ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
            ) {
                Text(text = parameter.name, style = SlimlyText.MonoSmall, color = SlimlyColor.Neutral500)
                Text(
                    text = parameter.value,
                    style = SlimlyText.MonoSmall,
                    color = SlimlyColor.Neutral300,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    SlimlyCard {
        Text(text = stringResource(R.string.analytics_empty), style = SlimlyText.RowTitle, color = SlimlyColor.Text)
        Text(
            text = stringResource(R.string.analytics_empty_note),
            style = SlimlyText.CaptionSmall,
            color = SlimlyColor.Neutral500,
            modifier = Modifier.padding(top = SlimlySpacing.X1),
        )
    }
}

private val BOTTOM_INSET = 40.dp
