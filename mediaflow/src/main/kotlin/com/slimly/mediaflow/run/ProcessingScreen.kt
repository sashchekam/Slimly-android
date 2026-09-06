package com.slimly.mediaflow.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slimly.core.model.MediaItem
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyLinkButton
import com.slimly.designsystem.SlimlyProgressBar
import com.slimly.designsystem.SlimlyProgressRing
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.mediaflow.MediaThumbnail
import com.slimly.mediaflow.R

/**
 * Обработка: кольцо с процентом и очередь файлов под ним.
 *
 * Уйти с экрана можно только отменой, но свернуть приложение — можно:
 * работа живёт в foreground-сервисе и продолжается с погашенным экраном,
 * а прогресс в это время виден в шторке.
 */
@Composable
fun ProcessingScreen(viewModel: ProcessingViewModel, modifier: Modifier = Modifier) {
    // Разрешение спрашиваем до запуска, а не после: уведомление — это всё,
    // что человек увидит, свернув приложение. Ответ не решает ничего:
    // откажут — работа пойдёт всё равно, просто молча.
    var isAsked by remember { mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) }
    val notifications = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isAsked = true }

    LaunchedEffect(Unit) {
        if (!isAsked) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(isAsked) {
        if (isAsked) viewModel.run()
    }

    // Системный «назад» — это отмена: работа останавливается явно, потому что
    // сама она уход с экрана переживёт.
    BackHandler(onBack = viewModel::cancel)

    SlimlyScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SlimlySpacing.ScreenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SlimlyProgressRing(
                value = viewModel.progress.overall.toFloat(),
                modifier = Modifier.padding(top = SlimlySpacing.X8),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.progress_percent, viewModel.percent),
                        style = SlimlyText.StatRing,
                        color = SlimlyColor.Text,
                    )
                    Text(text = viewModel.runWord, style = SlimlyText.Caption, color = SlimlyColor.Neutral500)
                }
            }

            Text(
                text = viewModel.remainingText
                    ?.let { stringResource(R.string.progress_remaining, it, viewModel.countText) }
                    ?: stringResource(R.string.progress_estimating, viewModel.countText),
                style = SlimlyText.Body,
                color = SlimlyColor.Neutral500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = SlimlySpacing.X6),
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = SlimlySpacing.X8),
                verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
            ) {
                items(items = viewModel.items, key = { it.id }) { item ->
                    ProcessingRow(item = item, viewModel = viewModel)
                }
            }

            SlimlyLinkButton(
                title = stringResource(R.string.progress_cancel),
                onClick = viewModel::cancel,
                modifier = Modifier.padding(bottom = SlimlySpacing.X6),
                color = SlimlyColor.Neutral400,
            )
        }
    }
}

@Composable
private fun ProcessingRow(item: MediaItem, viewModel: ProcessingViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MediaThumbnail(
            item = item,
            thumbnails = viewModel.thumbnails,
            size = THUMBNAIL_SIZE,
            modifier = Modifier
                .size(THUMBNAIL_SIDE)
                .clip(RoundedCornerShape(SlimlyRadius.Thumbnail)),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = SlimlySpacing.X4),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
        ) {
            Text(
                text = item.fileName,
                style = SlimlyText.RowTitle,
                color = SlimlyColor.Text,
                maxLines = 1,
            )
            SlimlyProgressBar(value = viewModel.fraction(item), height = 3.dp)
        }

        Box(Modifier.padding(start = SlimlySpacing.X3)) {
            Text(
                text = stringResource(R.string.progress_percent, (viewModel.fraction(item) * PERCENT).toInt()),
                style = SlimlyText.Caption,
                color = SlimlyColor.Neutral500,
            )
        }
    }
}

private const val THUMBNAIL_SIZE = 128
private const val PERCENT = 100
private val THUMBNAIL_SIDE = 44.dp
