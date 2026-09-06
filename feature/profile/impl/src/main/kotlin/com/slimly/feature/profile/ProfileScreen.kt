package com.slimly.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyBarFill
import com.slimly.designsystem.SlimlyCard
import com.slimly.designsystem.SlimlyCardDivider
import com.slimly.designsystem.SlimlyChevronRight
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyColumnDivider
import com.slimly.designsystem.SlimlyKicker
import com.slimly.designsystem.SlimlyLinkButton
import com.slimly.designsystem.SlimlyProgressBar
import com.slimly.designsystem.SlimlyRowGroup
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.SlimlyToastHost
import com.slimly.designsystem.slimlyInsetTile
import com.slimly.designsystem.slimlyRow
import com.slimly.designsystem.slimlyTap

/**
 * Профиль: что лежит в медиатеке, что мы из этого сжали и чем пользовались.
 *
 * Три карточки и список настроек — ровно тот же состав, что на iOS: экран
 * отвечает на «сколько я на этом выиграл», и добавлять к ответу нечего.
 */
@Composable
internal fun ProfileScreen(viewModel: ProfileViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(viewModel) { viewModel.load() }

    Box(modifier) {
        SlimlyScreen(onBack = viewModel::goBack) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SlimlySpacing.ScreenHorizontal)
                    .padding(bottom = SlimlySpacing.X6),
                verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X6),
            ) {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = SlimlyText.LargeTitle,
                    color = SlimlyColor.Text,
                )

                LibraryCard(viewModel)
                SavedCard(viewModel)
                UsageCard(viewModel)
                Settings(viewModel)
            }
        }

        SlimlyToastHost(viewModel.toast)
    }
}

// MARK: В медиатеке

@Composable
private fun LibraryCard(viewModel: ProfileViewModel) {
    SlimlyCard {
        SlimlyKicker(stringResource(R.string.profile_library))

        Row(
            modifier = Modifier.padding(top = SlimlySpacing.X4),
            horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X6),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatColumn(
                modifier = Modifier.weight(1f),
                value = viewModel.photoCountText,
                caption = viewModel.photoCaption,
            )
            SlimlyColumnDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                value = viewModel.videoCountText,
                caption = viewModel.videoCaption,
            )
            SlimlyColumnDivider()
            StatColumn(
                modifier = Modifier.weight(1f),
                value = viewModel.freeSpace.value,
                unit = viewModel.freeSpace.unit,
                caption = stringResource(R.string.profile_free),
            )
        }
    }
}

@Composable
private fun StatColumn(value: String, caption: String, modifier: Modifier = Modifier, unit: String? = null) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = SlimlyText.StatS, color = SlimlyColor.Text)
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = SlimlyText.Body,
                    color = SlimlyColor.Neutral400,
                    modifier = Modifier.padding(bottom = UNIT_BASELINE),
                )
            }
        }
        Text(
            text = caption,
            style = SlimlyText.CaptionSmall,
            color = SlimlyColor.Neutral500,
            modifier = Modifier.padding(top = SlimlySpacing.X1),
        )
    }
}

// MARK: Сжато нами

@Composable
private fun SavedCard(viewModel: ProfileViewModel) {
    SlimlyCard {
        SlimlyKicker(stringResource(R.string.profile_compressed))

        Row(
            modifier = Modifier.padding(top = SlimlySpacing.X3),
            horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text = viewModel.savedText, style = SlimlyText.StatM, color = SlimlyColor.Accent300)
            Text(
                text = stringResource(R.string.fact_freed),
                style = SlimlyText.BodySmall,
                color = SlimlyColor.Neutral400,
                modifier = Modifier.padding(bottom = UNIT_BASELINE),
            )
        }

        SlimlyProgressBar(
            value = viewModel.remainingFraction,
            height = BAR_HEIGHT,
            modifier = Modifier.padding(top = SlimlySpacing.X4),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SlimlySpacing.X3),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = viewModel.compressedText, style = SlimlyText.CaptionSmall, color = SlimlyColor.Neutral500)
            Text(text = viewModel.originalText, style = SlimlyText.CaptionSmall, color = SlimlyColor.Neutral500)
        }

        SlimlyCardDivider(Modifier.padding(top = SlimlySpacing.X4))

        Row(
            modifier = Modifier.padding(top = SlimlySpacing.X4),
            horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X6),
        ) {
            viewModel.breakdown.forEach { fact ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X1),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(text = fact.value, style = SlimlyText.RowTitle, color = SlimlyColor.Text)
                    Text(text = fact.caption, style = SlimlyText.CaptionSmall, color = SlimlyColor.Neutral500)
                }
            }
        }
    }
}

// MARK: Чем пользовались

@Composable
private fun UsageCard(viewModel: ProfileViewModel) {
    SlimlyCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SlimlyKicker(stringResource(R.string.profile_usage))
            Text(text = viewModel.usageCaption, style = SlimlyText.CaptionSmall, color = SlimlyColor.Neutral500)
        }

        // Сетка вручную парами, а не `LazyVerticalGrid`: ленивая сетка внутри
        // прокручиваемой колонки требует своей высоты, а плиток здесь ровно
        // четыре — считать их лениво нечего.
        Column(
            modifier = Modifier.padding(top = SlimlySpacing.X4),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        ) {
            viewModel.usage.chunked(TILES_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3)) {
                    row.forEach { tile ->
                        UsageTileView(
                            tile = tile,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.openStatistics(StatisticsSource.TILE) },
                        )
                    }
                }
            }
        }

        SlimlyLinkButton(
            title = stringResource(R.string.profile_all_statistics),
            onClick = { viewModel.openStatistics(StatisticsSource.LINK) },
            modifier = Modifier.padding(top = SlimlySpacing.X2),
        )
    }
}

@Composable
private fun UsageTileView(tile: UsageTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .slimlyInsetTile()
            .slimlyTap(onClick = onClick)
            .padding(horizontal = TILE_HORIZONTAL, vertical = TILE_VERTICAL),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = tile.countText,
                style = SlimlyText.StatTile,
                color = if (tile.isLeading) SlimlyColor.Accent200 else SlimlyColor.Neutral100,
            )
            Text(text = tile.unit, style = SlimlyText.Micro, color = SlimlyColor.Neutral500)
        }

        Text(text = tile.title, style = SlimlyText.Caption, color = SlimlyColor.Neutral300)

        SlimlyProgressBar(
            value = tile.share.toFloat(),
            height = TILE_BAR_HEIGHT,
            fill = if (tile.isLeading) SlimlyBarFill.SOLID else SlimlyBarFill.MUTED,
        )
    }
}

// MARK: Настройки

@Composable
private fun Settings(viewModel: ProfileViewModel) {
    val context = LocalContext.current

    SlimlyRowGroup {
        viewModel.settingsRows.forEach { row ->
            SettingsRowView(
                row = row,
                onClick = {
                    when (row.id) {
                        // Системный экран открывает Activity, а не вьюмодель:
                        // намерение — это платформа, и ей здесь самое место.
                        SettingsRowId.LANGUAGE -> {
                            viewModel.languageSettingsOpened()
                            context.startActivity(viewModel.languageSettings.intent(context.packageName))
                        }

                        SettingsRowId.VERSION -> viewModel.tapVersion()

                        SettingsRowId.ACCESS -> Unit
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsRowView(row: SettingsRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .slimlyTap(enabled = row.isTappable, onClick = onClick)
            .slimlyRow(),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.label,
            style = SlimlyText.Body,
            color = SlimlyColor.Text,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.value,
            style = SlimlyText.CaptionSmall,
            color = SlimlyColor.Neutral500,
            textAlign = TextAlign.End,
        )
        // Шеврон одинаковый у всех строк: он часть ритма списка, а не
        // обещание перехода. Спрятать его у строки версии значило бы
        // подсветить ту единственную, которая куда-то ведёт.
        SlimlyChevronRight()
    }
}

private val UNIT_BASELINE = 2.dp
private val BAR_HEIGHT = 8.dp
private val TILE_HORIZONTAL = 13.dp
private val TILE_VERTICAL = 11.dp
private val TILE_BAR_HEIGHT = 3.dp
private const val TILES_PER_ROW = 2
