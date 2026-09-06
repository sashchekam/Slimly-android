package com.slimly.mediaflow.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.core.model.MediaItem
import com.slimly.designsystem.SlimlyArrowRight
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyCard
import com.slimly.designsystem.SlimlyChip
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRowGroup
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyRow
import com.slimly.designsystem.slimlyTap
import com.slimly.mediaflow.MediaFileRow
import com.slimly.mediaflow.R

/**
 * Общий шаг «в какой формат»: пара «из → в», выбор формата и список файлов.
 *
 * Пара наверху — главное, что здесь нужно понять: человек пришёл не выбирать
 * настройки, а поменять формат, и видеть он должен именно эту замену.
 */
@Composable
fun FormatStepScreen(viewModel: FormatStepViewModel, modifier: Modifier = Modifier) {
    SlimlyScreen(
        modifier = modifier,
        onBack = viewModel::goBack,
        bottomAction = { SlimlyButton(title = viewModel.ctaTitle, onClick = viewModel::start) },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SlimlySpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X6),
        ) {
            Header(viewModel)
            TransitionCard(viewModel)

            Text(
                text = stringResource(R.string.format_section_title),
                style = SlimlyText.SectionTitle,
                color = SlimlyColor.Text,
            )
            Formats(viewModel)

            Text(text = viewModel.config.note, style = SlimlyText.CaptionSmall, color = SlimlyColor.Neutral600)

            FileList(viewModel)

            // Под последней строкой — место под приклеенную кнопку: иначе
            // список уезжает прямо ей под низ.
            Spacer(Modifier.height(BOTTOM_INSET))
        }
    }
}

@Composable
private fun Header(viewModel: FormatStepViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2)) {
        Text(text = viewModel.config.title, style = SlimlyText.Title, color = SlimlyColor.Text)
        Text(text = viewModel.summary, style = SlimlyText.BodySmall, color = SlimlyColor.Neutral400)
    }
}

/** Пара «из → в»: слева то, что было, справа то, что выбрано сейчас. */
@Composable
private fun TransitionCard(viewModel: FormatStepViewModel) {
    SlimlyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormatColumn(
                kicker = stringResource(R.string.format_from),
                value = viewModel.config.sourceFormat,
                color = SlimlyColor.Neutral300,
                modifier = Modifier.weight(1f),
            )

            SlimlyArrowRight(modifier = Modifier.padding(horizontal = SlimlySpacing.X4))

            FormatColumn(
                kicker = stringResource(R.string.format_to),
                value = viewModel.targetTitle,
                color = SlimlyColor.Accent300,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FormatColumn(kicker: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3)) {
        Text(text = kicker.uppercase(), style = SlimlyText.Kicker, color = SlimlyColor.Neutral500)
        Text(text = value, style = SlimlyText.FormatValue, color = color)
    }
}

@Composable
private fun Formats(viewModel: FormatStepViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
    ) {
        viewModel.formats.forEach { format ->
            SlimlyChip(
                title = format.shortName,
                isSelected = viewModel.isSelected(format),
                onClick = { viewModel.select(format) },
            )
        }
    }
}

@Composable
private fun FileList(viewModel: FormatStepViewModel) {
    SlimlyRowGroup {
        viewModel.items.forEach { item ->
            FileRow(item = item, viewModel = viewModel)
        }
    }
}

/**
 * Строка файла. Нажимается целиком, а не по миниатюре: промежутки между
 * кадром и подписями проваливались бы мимо, хотя выглядят частью строки.
 */
@Composable
private fun FileRow(item: MediaItem, viewModel: FormatStepViewModel) {
    MediaFileRow(
        item = item,
        name = item.fileName,
        detail = viewModel.detail(item),
        thumbnails = viewModel.thumbnails,
        modifier = Modifier
            .slimlyTap(enabled = viewModel.allowsPreview) { viewModel.openPreview(item) }
            .slimlyRow(),
    )
}

private val BOTTOM_INSET = 96.dp
