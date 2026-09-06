package com.slimly.feature.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyCard
import com.slimly.designsystem.SlimlyChevronRight
import com.slimly.designsystem.SlimlyChip
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyKicker
import com.slimly.designsystem.SlimlyLinkButton
import com.slimly.designsystem.SlimlyRowGroup
import com.slimly.designsystem.SlimlyScreenBackground
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.SlimlyToastHost
import com.slimly.designsystem.slimlyRow
import com.slimly.designsystem.slimlyTap

/**
 * Сервисный экран, а не часть продукта: здесь подменяют поведение сборки
 * и забирают данные о ней.
 *
 * Шапка своя, а не `SlimlyScreen`: у меню есть заголовок и «Готово» вместо
 * шеврона — это слой поверх всего, из него не возвращаются, его закрывают.
 */
@Composable
fun DebugMenuScreen(viewModel: DebugMenuViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { viewModel.load() }

    Box(modifier.fillMaxSize()) {
        SlimlyScreenBackground()

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(viewModel)

            LazyColumn(
                contentPadding = PaddingValues(
                    start = SlimlySpacing.ScreenHorizontal,
                    end = SlimlySpacing.ScreenHorizontal,
                    bottom = BOTTOM_INSET,
                ),
                verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
            ) {
                item {
                    Text(text = viewModel.summary, style = SlimlyText.Caption, color = SlimlyColor.Neutral500)
                }

                items(items = viewModel.knobs, key = { it.id.name }) { knob -> KnobCard(knob, viewModel) }

                item { SlimlyKicker(stringResource(R.string.menu_sections)) }
                item { Sections(viewModel) }

                item { SlimlyKicker(stringResource(R.string.menu_actions)) }
                item { Actions(viewModel) }

                item {
                    Text(
                        text = stringResource(R.string.menu_note),
                        style = SlimlyText.CaptionSmall,
                        color = SlimlyColor.Neutral600,
                    )
                }
            }
        }

        SlimlyToastHost(viewModel.toast)
    }
}

/** Заголовок, метка сборки и «Готово» — вместо обычной шапки с шевроном. */
@Composable
private fun Header(viewModel: DebugMenuViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HEADER_HEIGHT)
            .padding(horizontal = SlimlySpacing.ScreenHorizontal)
            .padding(bottom = SlimlySpacing.X3),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.menu_title), style = SlimlyText.SectionTitle, color = SlimlyColor.Text)
        DebugTag(INTERNAL)

        Box(Modifier.weight(1f))

        SlimlyLinkButton(title = stringResource(R.string.menu_close), onClick = viewModel::close)
    }
}

@Composable
private fun KnobCard(knob: DebugKnob, viewModel: DebugMenuViewModel) {
    SlimlyCard {
        Text(text = knob.title, style = SlimlyText.RowTitle, color = SlimlyColor.Text)
        Text(
            text = knob.hint,
            style = SlimlyText.CaptionSmall,
            color = SlimlyColor.Neutral500,
            modifier = Modifier.padding(top = SlimlySpacing.X1),
        )

        // Вариантов у крутилки бывает и пять: в один ряд они не помещаются,
        // а горизонтальная прокрутка внутри карточки прятала бы выбранный.
        FlowRow(
            modifier = Modifier.padding(top = SlimlySpacing.X3),
            horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
        ) {
            knob.options.forEach { option ->
                SlimlyChip(
                    title = option.title,
                    isSelected = option.isSelected,
                    onClick = { viewModel.select(option, knob) },
                )
            }
        }
    }
}

@Composable
private fun Sections(viewModel: DebugMenuViewModel) {
    SlimlyRowGroup {
        viewModel.sections.forEach { section ->
            Row(
                modifier = Modifier
                    .slimlyTap { viewModel.open(section) }
                    .slimlyRow(),
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = viewModel.title(section),
                    style = SlimlyText.RowTitle,
                    color = SlimlyColor.Text,
                    modifier = Modifier.weight(1f),
                )
                Text(text = viewModel.value(section), style = SlimlyText.Caption, color = SlimlyColor.Neutral600)
                SlimlyChevronRight()
            }
        }
    }
}

@Composable
private fun Actions(viewModel: DebugMenuViewModel) {
    SlimlyRowGroup {
        viewModel.actions.forEach { action ->
            Row(
                modifier = Modifier
                    .slimlyTap { viewModel.perform(action) }
                    .slimlyRow(),
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(action.titleRes),
                    style = SlimlyText.RowTitle,
                    color = SlimlyColor.Text,
                    modifier = Modifier.weight(1f),
                )
                Text(text = stringResource(action.hintRes), style = SlimlyText.Caption, color = SlimlyColor.Neutral600)
            }
        }
    }
}

private const val INTERNAL = "INTERNAL"
private val HEADER_HEIGHT = 44.dp
private val BOTTOM_INSET = 40.dp
