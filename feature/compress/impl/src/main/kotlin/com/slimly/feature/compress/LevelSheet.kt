package com.slimly.feature.compress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.slimly.core.model.CompressionPreset
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap

/**
 * Шторка со степенью сжатия.
 *
 * Пресеты — это засечки на непрерывной шкале, а не три отдельных режима:
 * между ними коэффициент интерполируется, поэтому промежуточное положение
 * ползунка тоже имеет смысл.
 */
@Composable
fun LevelSheet(viewModel: LevelSheetViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = SlimlySpacing.ScreenHorizontal)
            .padding(bottom = SlimlySpacing.X8),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
    ) {
        Text(
            text = stringResource(R.string.level_title),
            style = SlimlyText.Title,
            color = SlimlyColor.Text,
        )

        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = viewModel.afterText, style = SlimlyText.StatL, color = SlimlyColor.Text)
            Text(
                text = viewModel.deltaText,
                style = SlimlyText.SectionTitle,
                color = SlimlyColor.Accent300,
                modifier = Modifier.padding(start = SlimlySpacing.X3, bottom = SlimlySpacing.X2),
            )
        }
        Text(
            text = stringResource(R.string.level_from, viewModel.beforeText),
            style = SlimlyText.Caption,
            color = SlimlyColor.Neutral500,
        )

        Slider(
            value = viewModel.level.value.toFloat(),
            onValueChange = viewModel::setLevel,
            colors = SliderDefaults.colors(
                thumbColor = SlimlyColor.Accent,
                activeTrackColor = SlimlyColor.Accent,
                inactiveTrackColor = SlimlyColor.Neutral900,
            ),
        )

        PresetRow(viewModel)

        Text(
            text = stringResource(viewModel.level.nearestPreset.summaryRes),
            style = SlimlyText.Caption,
            color = SlimlyColor.Neutral500,
        )

        SlimlyButton(
            title = stringResource(R.string.level_action, viewModel.selectionText),
            onClick = viewModel::start,
            modifier = Modifier.padding(top = SlimlySpacing.X3),
        )
    }
}

@Composable
private fun PresetRow(viewModel: LevelSheetViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        viewModel.presets.forEach { preset ->
            PresetLabel(
                preset = preset,
                isNearest = viewModel.isNearest(preset),
                onClick = { viewModel.select(preset) },
            )
        }
    }
}

@Composable
private fun PresetLabel(preset: CompressionPreset, isNearest: Boolean, onClick: () -> Unit) {
    Text(
        text = stringResource(preset.titleRes),
        style = SlimlyText.CaptionSmall,
        color = if (isNearest) SlimlyColor.Accent200 else SlimlyColor.Neutral600,
        modifier = Modifier
            .slimlyTap(onClick = onClick)
            .padding(vertical = SlimlySpacing.X2),
    )
}
