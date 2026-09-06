package com.slimly.feature.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyCamera
import com.slimly.designsystem.SlimlyChevronRight
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyGallery
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap

/**
 * Выбор источника страниц: камера или галерея.
 *
 * Шторка, а не экран: выбрать источник — это одно касание, и ради него
 * уводить с хаба незачем. По тапу шторка уходит, а на её месте
 * открывается уже сам источник.
 */
@Composable
fun ScanIntroSheet(viewModel: ScanIntroViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = SlimlySpacing.ScreenHorizontal)
            .padding(bottom = SlimlySpacing.X8),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
    ) {
        Text(
            text = stringResource(R.string.intro_title),
            style = SlimlyText.Title,
            color = SlimlyColor.Text,
        )
        Text(
            text = stringResource(R.string.intro_note),
            style = SlimlyText.BodySmall,
            color = SlimlyColor.Neutral400,
        )

        SourceRow(
            title = stringResource(R.string.intro_camera),
            subtitle = stringResource(R.string.intro_camera_hint),
            isAccented = true,
            onClick = viewModel::openCamera,
            icon = { SlimlyCamera(color = SlimlyColor.Accent300) },
        )

        SourceRow(
            title = stringResource(R.string.intro_library),
            subtitle = stringResource(R.string.intro_library_hint, PAGE_LIMIT),
            isAccented = false,
            onClick = viewModel::openGallery,
            icon = { SlimlyGallery(color = SlimlyColor.Neutral300) },
        )
    }
}

/**
 * Строка источника. Камера выделена акцентом: сканируют обычно то, что
 * лежит перед человеком прямо сейчас, — а из галереи собирают уже снятое.
 */
@Composable
private fun SourceRow(
    title: String,
    subtitle: String,
    isAccented: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(SlimlyRadius.Lg)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isAccented) SlimlyColor.Accent.copy(alpha = ACCENT_FILL) else SlimlyColor.Surface,
                shape,
            )
            .then(
                if (isAccented) Modifier.border(EDGE, SlimlyColor.Accent600, shape) else Modifier,
            )
            .slimlyTap(onClick = onClick)
            .padding(SlimlySpacing.X6),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X1),
        ) {
            Text(
                text = title,
                style = SlimlyText.SectionTitle,
                color = if (isAccented) SlimlyColor.Accent100 else SlimlyColor.Text,
            )
            Text(
                text = subtitle,
                style = SlimlyText.Caption,
                color = if (isAccented) SlimlyColor.Neutral400 else SlimlyColor.Neutral500,
            )
        }

        SlimlyChevronRight(color = if (isAccented) SlimlyColor.Accent400 else SlimlyColor.Neutral600)
    }
}

private val EDGE = 1.dp
private const val ACCENT_FILL = 0.1f
