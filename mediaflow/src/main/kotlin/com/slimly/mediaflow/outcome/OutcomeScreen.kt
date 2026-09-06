package com.slimly.mediaflow.outcome

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slimly.core.model.ProcessingFailure
import com.slimly.core.model.ProcessingResult
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyButtonEmphasis
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyLinkButton
import com.slimly.designsystem.SlimlyPencil
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlyShare
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap
import com.slimly.mediaflow.R

/**
 * Итог: что получилось, куда оно уедет и что делать с оригиналами.
 *
 * Возврата в стек отсюда нет — экран обработки из истории убран, и «назад»
 * ведёт на хаб.
 */
@Composable
fun OutcomeScreen(viewModel: OutcomeViewModel, modifier: Modifier = Modifier) {
    val deletion = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result -> viewModel.deleteAnswered(result.resultCode == Activity.RESULT_OK) }

    LaunchedEffect(viewModel.deleteRequest) {
        val request = viewModel.deleteRequest ?: return@LaunchedEffect
        viewModel.deleteLaunched()
        deletion.launch(IntentSenderRequest.Builder(request).build())
    }

    SlimlyScreen(
        modifier = modifier,
        bottomAction = { OutcomeAction(viewModel) },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = SlimlySpacing.ScreenHorizontal,
                end = SlimlySpacing.ScreenHorizontal,
                bottom = BOTTOM_INSET,
            ),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        ) {
            item { OutcomeHeadline(viewModel) }

            items(items = viewModel.results, key = { it.item.id }) { result ->
                ResultRow(result = result, viewModel = viewModel)
            }

            if (viewModel.showsShareAll) {
                item { ShareAll(viewModel) }
            }

            viewModel.previewHint?.let { hint ->
                item {
                    Text(
                        text = hint,
                        style = SlimlyText.Caption,
                        color = SlimlyColor.Neutral600,
                        modifier = Modifier.padding(top = SlimlySpacing.X1),
                    )
                }
            }

            if (viewModel.failures.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.outcome_failed),
                        style = SlimlyText.SectionTitle,
                        color = SlimlyColor.Neutral400,
                        modifier = Modifier.padding(top = SlimlySpacing.X4),
                    )
                }
                items(items = viewModel.failures, key = { it.item.id }) { failure -> FailureRow(failure) }
            }
        }
    }

    Toast(viewModel)

    if (viewModel.isRenaming) {
        RenameDialog(viewModel)
    }
}

@Composable
private fun OutcomeHeadline(viewModel: OutcomeViewModel) {
    Column(Modifier.padding(vertical = SlimlySpacing.X6)) {
        Text(
            text = viewModel.kicker.uppercase(),
            style = SlimlyText.Kicker,
            color = SlimlyColor.Neutral500,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = SlimlySpacing.X2),
        ) {
            Text(text = viewModel.beforeText, style = SlimlyText.StatStruck, color = SlimlyColor.Neutral600)
            Text(
                text = " → ",
                style = SlimlyText.StatStruck,
                color = SlimlyColor.Neutral600,
            )
            Text(text = viewModel.afterText, style = SlimlyText.StatXL, color = SlimlyColor.Text)
        }
        Text(
            text = viewModel.summary,
            style = SlimlyText.Body,
            color = SlimlyColor.Neutral500,
            modifier = Modifier.padding(top = SlimlySpacing.X3),
        )
    }
}

/**
 * Строка результата: нажатие открывает просмотр, карандаш правит имя.
 *
 * Нажимается вся строка, а не одна надпись: промежутки между текстом
 * и дельтой проваливались бы мимо, хотя выглядят её частью. Карандаш при
 * этом свой — он лежит поверх строки и перехватывает нажатие себе.
 */
@Composable
private fun ResultRow(result: ProcessingResult, viewModel: OutcomeViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlimlyRadius.Lg))
            .background(SlimlyColor.SurfaceInset)
            .slimlyTap(enabled = viewModel.canPreview(result)) { viewModel.openPreview(result) }
            .padding(SlimlySpacing.X4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = result.outputName,
                style = SlimlyText.RowTitle,
                color = SlimlyColor.Text,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            Text(
                text = viewModel.detail(result),
                style = SlimlyText.Caption,
                color = SlimlyColor.Neutral500,
                modifier = Modifier.padding(top = SlimlySpacing.X1),
            )
        }

        // Имя правят до сохранения: менять его у копии, уже разложенной
        // по месту, приложение не умеет.
        if (viewModel.canRename) {
            val label = stringResource(R.string.outcome_rename_file, result.outputName)

            Box(
                modifier = Modifier
                    .size(TAP_TARGET)
                    .clip(CircleShape)
                    .slimlyTap { viewModel.rename(result) }
                    .semantics { contentDescription = label },
                contentAlignment = Alignment.Center,
            ) {
                SlimlyPencil()
            }
        }

        if (viewModel.canShare(result)) {
            ShareFile(result = result, viewModel = viewModel)
        }

        Text(text = viewModel.delta(result), style = SlimlyText.SectionTitle, color = SlimlyColor.Accent300)
    }
}

/**
 * Отдать один файл наружу. Значок свой, как и карандаш: он лежит поверх
 * строки и перехватывает нажатие себе, не открывая просмотр.
 */
@Composable
private fun ShareFile(result: ProcessingResult, viewModel: OutcomeViewModel) {
    val context = LocalContext.current
    val label = viewModel.shareFileLabel(result)

    Box(
        modifier = Modifier
            .size(TAP_TARGET)
            .clip(CircleShape)
            .slimlyTap { viewModel.shareIntent(result)?.let(context::startActivity) }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        SlimlyShare()
    }
}

/**
 * Одна шторка на все результаты: по отдельности их пришлось бы отправлять
 * столько же раз, сколько файлов в очереди.
 */
@Composable
private fun ShareAll(viewModel: OutcomeViewModel) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SlimlyRadius.Md))
            .background(SlimlyColor.SurfaceInset)
            .slimlyTap { viewModel.shareAllIntent()?.let(context::startActivity) }
            .padding(vertical = SlimlySpacing.X3),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlimlyShare()
        Text(
            text = viewModel.shareAllTitle,
            style = SlimlyText.Button,
            color = SlimlyColor.Neutral100,
            modifier = Modifier.padding(start = SlimlySpacing.X2),
        )
    }
}

@Composable
private fun FailureRow(failure: ProcessingFailure) {
    Column(Modifier.padding(vertical = SlimlySpacing.X1)) {
        Text(text = failure.item.fileName, style = SlimlyText.RowTitle, color = SlimlyColor.Neutral400, maxLines = 1)
        Text(text = failure.message, style = SlimlyText.Caption, color = SlimlyColor.Neutral600)
    }
}

@Composable
private fun OutcomeAction(viewModel: OutcomeViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2)) {
        if (viewModel.showsSaveButton) {
            SlimlyButton(
                title = stringResource(viewModel.saveTitle),
                onClick = viewModel::save,
                enabled = !viewModel.isSaving,
            )
        } else {
            SlimlyButton(
                title = stringResource(R.string.outcome_done),
                onClick = viewModel::finish,
                emphasis = SlimlyButtonEmphasis.PLAIN,
            )
        }

        // Подпись остаётся и после сохранения: когда тост погаснет, это
        // единственное место, где написано, куда легли файлы.
        if (viewModel.showsSaveNote) {
            Text(
                text = stringResource(viewModel.saveNote),
                style = SlimlyText.Caption,
                color = SlimlyColor.Neutral600,
                modifier = Modifier.padding(top = SlimlySpacing.X1),
            )
        }

        if (viewModel.showsDeleteOriginals) {
            SlimlyLinkButton(
                title = stringResource(R.string.outcome_delete_originals, viewModel.deleteSubtitle),
                onClick = viewModel::deleteOriginals,
                color = SlimlyColor.Neutral400,
            )
        }
    }
}

/** Подтверждение сохранения: пара секунд поверх экрана и без кнопки закрытия. */
@Composable
private fun Toast(viewModel: OutcomeViewModel) {
    val text = viewModel.toast

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = text != null) {
            Text(
                text = text?.let { stringResource(it) }.orEmpty(),
                style = SlimlyText.BodySmall,
                color = SlimlyColor.Text,
                modifier = Modifier
                    .padding(bottom = TOAST_INSET)
                    .background(SlimlyColor.Menu, RoundedCornerShape(SlimlyRadius.Pill))
                    .padding(horizontal = SlimlySpacing.X6, vertical = SlimlySpacing.X3),
            )
        }
    }
}

private val TAP_TARGET = 34.dp
private val BOTTOM_INSET = 160.dp
private val TOAST_INSET = 140.dp
