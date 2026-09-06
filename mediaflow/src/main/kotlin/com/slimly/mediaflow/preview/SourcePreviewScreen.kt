package com.slimly.mediaflow.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText

/**
 * Просмотр исходного файла: кадр целиком, имя и то, что о файле известно.
 *
 * Плеера у ролика нет, в отличие от iOS: там просмотр отдают AVKit, а здесь
 * он показал бы кадр из того же места, что и мы, — ради движения человек
 * откроет файл системным приложением.
 */
@Composable
fun SourcePreviewScreen(viewModel: SourcePreviewViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(viewModel.item.id) { viewModel.load() }

    SlimlyScreen(modifier = modifier, onBack = viewModel::goBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SlimlySpacing.ScreenHorizontal, vertical = SlimlySpacing.X3),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X6),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2)) {
                Text(
                    text = viewModel.title,
                    style = SlimlyText.Title,
                    color = SlimlyColor.Text,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                )
                Text(text = viewModel.subtitle, style = SlimlyText.Caption, color = SlimlyColor.Neutral500)
            }

            PreviewFrame(
                frame = viewModel.frame,
                placeholder = viewModel.placeholderLabel,
                // Кадр вписываем целиком: на просмотре важно, что на снимке,
                // а обрезка по окну прячет как раз края.
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    // Кадр уже ширины экрана прижался бы влево, а подписи
                    // под ним остались бы во всю ширину.
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    // Выше кадр не растёт: вертикальный снимок иначе выдавливал
                    // бы подписи за нижний край, и о файле опять пришлось бы
                    // судить по одной картинке.
                    .heightIn(max = MAX_HEIGHT)
                    .aspectRatio(viewModel.previewAspectRatio)
                    .clip(RoundedCornerShape(SlimlyRadius.Lg)),
            )

            Text(text = viewModel.note, style = SlimlyText.Caption, color = SlimlyColor.Neutral600)
        }
    }
}

private val MAX_HEIGHT = 460.dp
