package com.slimly.feature.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyButtonEmphasis
import com.slimly.designsystem.SlimlyCard
import com.slimly.designsystem.SlimlyChevronRight
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyLinkButton
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyRowGroup
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.SlimlyToastHost
import com.slimly.designsystem.slimlyRow
import com.slimly.designsystem.slimlyTap

/**
 * Значения удалённого конфига: что приехало с сервера и чем это подменено
 * на этом устройстве.
 */
@Composable
fun DebugRemoteConfigScreen(viewModel: DebugRemoteConfigViewModel, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        SlimlyScreen(
            onBack = viewModel::goBack,
            trailing = {
                SlimlyLinkButton(
                    title = stringResource(R.string.config_refresh),
                    onClick = viewModel::refresh,
                    enabled = !viewModel.isRefreshing,
                )
            },
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
                    Text(text = TITLE, style = SlimlyText.Title, color = SlimlyColor.Text)
                }
                item {
                    Text(
                        text = stringResource(R.string.config_note),
                        style = SlimlyText.Caption,
                        color = SlimlyColor.Neutral500,
                    )
                }

                if (entries.isEmpty()) {
                    item { EmptyCard() }
                } else {
                    item { Values(entries, viewModel) }
                }

                if (viewModel.hasOverrides) {
                    item {
                        SlimlyButton(
                            title = stringResource(R.string.config_clear_all),
                            onClick = viewModel::clearAll,
                            emphasis = SlimlyButtonEmphasis.PLAIN,
                        )
                    }
                }
            }
        }

        SlimlyToastHost(viewModel.toast)
    }

    if (viewModel.isEditing) {
        EditDialog(viewModel)
    }
}

@Composable
private fun Values(entries: List<DebugConfigEntry>, viewModel: DebugRemoteConfigViewModel) {
    SlimlyRowGroup {
        entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .slimlyTap { viewModel.edit(entry) }
                    .slimlyRow(),
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = entry.name, style = SlimlyText.Mono, color = SlimlyColor.Text)
                    Text(
                        text = viewModel.source(entry),
                        style = SlimlyText.CaptionSmall,
                        color = SlimlyColor.Neutral600,
                        modifier = Modifier.padding(top = SlimlySpacing.X1),
                    )
                }

                // Длинному значению — половина строки: имя ключа должно
                // остаться читаемым, даже если в значении лежит JSON.
                Text(
                    text = entry.value,
                    style = SlimlyText.Mono,
                    color = if (entry.isOverridden) SlimlyColor.Accent300 else SlimlyColor.Neutral400,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = VALUE_WIDTH),
                )

                SlimlyChevronRight(modifier = Modifier.padding(top = SlimlySpacing.X1))
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    SlimlyCard {
        Text(text = stringResource(R.string.config_empty), style = SlimlyText.RowTitle, color = SlimlyColor.Text)
        Text(
            text = stringResource(R.string.config_empty_note),
            style = SlimlyText.CaptionSmall,
            color = SlimlyColor.Neutral500,
            modifier = Modifier.padding(top = SlimlySpacing.X1),
        )
    }
}

/**
 * Правка значения. Своё окно, а не системный `AlertDialog`: тот приезжает
 * со своей типографикой и посреди Nocturne читается как чужой экран.
 */
@Composable
private fun EditDialog(viewModel: DebugRemoteConfigViewModel) {
    val focus = remember { FocusRequester() }

    Dialog(onDismissRequest = viewModel::cancelEdit) {
        Column(
            modifier = Modifier
                .background(SlimlyColor.Sheet, RoundedCornerShape(SlimlyRadius.Lg))
                .padding(DIALOG_PADDING),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        ) {
            Text(text = viewModel.editedTitle, style = SlimlyText.Mono, color = SlimlyColor.Text)

            ValueField(viewModel = viewModel, modifier = Modifier.focusRequester(focus))

            Text(
                text = stringResource(R.string.config_edit_note),
                style = SlimlyText.Caption,
                color = SlimlyColor.Neutral600,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlimlyButton(
                    title = stringResource(R.string.config_cancel),
                    onClick = viewModel::cancelEdit,
                    emphasis = SlimlyButtonEmphasis.PLAIN,
                    modifier = Modifier.weight(1f),
                )
                SlimlyButton(
                    title = stringResource(R.string.config_override),
                    onClick = viewModel::applyEdit,
                    modifier = Modifier.weight(1f),
                )
            }

            if (viewModel.canRestore) {
                SlimlyLinkButton(
                    title = stringResource(R.string.config_restore),
                    onClick = viewModel::restore,
                    color = SlimlyColor.Neutral400,
                )
            }
        }
    }

    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun ValueField(viewModel: DebugRemoteConfigViewModel, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(SlimlyRadius.Md)

    BasicTextField(
        value = viewModel.draft,
        onValueChange = { viewModel.draft = it },
        modifier = modifier
            .fillMaxWidth()
            .background(SlimlyColor.Background, shape)
            .border(EDGE, SlimlyColor.Neutral800, shape)
            .padding(horizontal = SlimlySpacing.X4, vertical = SlimlySpacing.X3),
        textStyle = SlimlyText.Mono.copy(color = SlimlyColor.Text),
        singleLine = true,
        cursorBrush = SolidColor(SlimlyColor.Accent300),
        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.applyEdit() }),
        decorationBox = { field ->
            if (viewModel.draft.isEmpty()) {
                Text(
                    text = stringResource(R.string.config_value_field),
                    style = SlimlyText.Mono,
                    color = SlimlyColor.Neutral600,
                )
            }
            field()
        },
    )
}

private const val TITLE = "Remote Config"
private val BOTTOM_INSET = 40.dp
private val VALUE_WIDTH = 120.dp
private val DIALOG_PADDING = 20.dp
private val EDGE = 1.dp
