package com.slimly.mediaflow.outcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyButtonEmphasis
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.mediaflow.R

/**
 * Правка имени готового файла.
 *
 * Своё окно, а не системный `AlertDialog`: тот приезжает со своей типографикой
 * и своими полями, и посреди Nocturne читается как чужой экран.
 */
@Composable
internal fun RenameDialog(viewModel: OutcomeViewModel) {
    val focus = remember { FocusRequester() }

    // Закрыть окно может и система — нажатием мимо него, поэтому закрытие
    // снимает правку, а не только кнопка «Отмена».
    Dialog(onDismissRequest = viewModel::cancelRename) {
        Column(
            modifier = Modifier
                .background(SlimlyColor.Sheet, RoundedCornerShape(SlimlyRadius.Lg))
                .padding(PADDING),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        ) {
            Text(text = stringResource(R.string.rename_title), style = SlimlyText.Title, color = SlimlyColor.Text)

            NameField(viewModel = viewModel, modifier = Modifier.focusRequester(focus))

            viewModel.renameNote?.let { note ->
                Text(text = note, style = SlimlyText.Caption, color = SlimlyColor.Neutral600)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlimlyButton(
                    title = stringResource(R.string.rename_cancel),
                    onClick = viewModel::cancelRename,
                    emphasis = SlimlyButtonEmphasis.PLAIN,
                    modifier = Modifier.weight(1f),
                )
                SlimlyButton(
                    title = stringResource(R.string.rename_apply),
                    onClick = viewModel::applyRename,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // Окно открывают ради ввода: клавиатура должна приехать вместе с ним,
    // а не по отдельному нажатию в поле.
    LaunchedEffect(Unit) { focus.requestFocus() }
}

@Composable
private fun NameField(viewModel: OutcomeViewModel, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(SlimlyRadius.Md)

    BasicTextField(
        value = viewModel.nameDraft,
        onValueChange = { viewModel.nameDraft = it },
        modifier = modifier
            .fillMaxWidth()
            .background(SlimlyColor.Background, shape)
            .border(EDGE, SlimlyColor.Neutral800, shape)
            .padding(horizontal = SlimlySpacing.X4, vertical = SlimlySpacing.X3),
        textStyle = SlimlyText.RowTitle.copy(color = SlimlyColor.Text),
        singleLine = true,
        cursorBrush = SolidColor(SlimlyColor.Accent300),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.applyRename() }),
        decorationBox = { field ->
            if (viewModel.nameDraft.isEmpty()) {
                Text(
                    text = stringResource(R.string.rename_field),
                    style = SlimlyText.RowTitle,
                    color = SlimlyColor.Neutral600,
                )
            }
            field()
        },
    )
}

private val PADDING = 20.dp
private val EDGE = 1.dp
