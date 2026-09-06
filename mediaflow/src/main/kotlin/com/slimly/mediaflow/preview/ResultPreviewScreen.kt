package com.slimly.mediaflow.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyArrowRight
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyCard
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyCompareSlider
import com.slimly.designsystem.SlimlyDeltaBadge
import com.slimly.designsystem.SlimlyFrameCaption
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySegmentedControl
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText

/**
 * Просмотр готового файла: у сжатого снимка — шторка «до / после»,
 * у сжатого ролика — переключатель сторон, у остальных инструментов —
 * один кадр результата.
 *
 * Ролик и документ показываем кадром, а не своим проигрывателем, как на iOS:
 * плеер и читалку сюда пришлось бы тащить целиком, а открыть файл человек
 * может системным приложением — так на Android это и делают.
 */
@Composable
fun ResultPreviewScreen(viewModel: ResultPreviewViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(viewModel.result.outputName) { viewModel.load() }

    SlimlyScreen(modifier = modifier, onBack = viewModel::goBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SlimlySpacing.ScreenHorizontal, vertical = SlimlySpacing.X3),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        ) {
            Header(viewModel)

            if (viewModel.showsSidePicker) {
                SlimlySegmentedControl(
                    options = PreviewSide.entries,
                    selected = viewModel.side,
                    title = { side ->
                        if (side == PreviewSide.BEFORE) viewModel.beforeTitle else viewModel.afterTitle
                    },
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = { viewModel.side = it },
                )
            }

            Window(viewModel)
            Sizes(viewModel)

            Text(text = viewModel.note, style = SlimlyText.Caption, color = SlimlyColor.Neutral600)

            OpenElsewhere(viewModel)
        }
    }
}

/**
 * Переход в системное приложение — единственный способ по-настоящему
 * посмотреть документ и послушать дорожку: в самом просмотре стоит кадр,
 * а не проигрыватель.
 */
@Composable
private fun OpenElsewhere(viewModel: ResultPreviewViewModel) {
    val context = LocalContext.current
    val intent = viewModel.openIntent ?: return

    SlimlyButton(title = viewModel.openTitle, onClick = { context.startActivity(intent) })
}

@Composable
private fun Header(viewModel: ResultPreviewViewModel) {
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
}

/** Окно просмотра вместе с подписями сторон поверх него. */
@Composable
private fun Window(viewModel: ResultPreviewViewModel) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(viewModel.previewAspectRatio)
            .clip(RoundedCornerShape(SlimlyRadius.Lg)),
    ) {
        if (viewModel.showsComparison) {
            Comparison(viewModel)
        } else {
            PreviewFrame(
                frame = viewModel.currentFrame,
                placeholder = viewModel.placeholderLabel,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Captions(viewModel)
    }
}

/**
 * Оба кадра в одном окне, а не по очереди: разница между оригиналом
 * и сжатой копией держится в мелочах, и, подменяя картинку целиком,
 * её приходится держать в памяти.
 */
@Composable
private fun Comparison(viewModel: ResultPreviewViewModel) {
    SlimlyCompareSlider(
        position = viewModel.comparePosition,
        modifier = Modifier.fillMaxSize(),
        onPosition = { viewModel.comparePosition = it },
        before = {
            PreviewFrame(
                frame = viewModel.beforeFrame,
                placeholder = viewModel.beforePlaceholderLabel,
                modifier = Modifier.fillMaxSize(),
            )
        },
        after = {
            PreviewFrame(
                frame = viewModel.afterFrame,
                placeholder = viewModel.placeholderLabel,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

/**
 * Подписи сторон. У шторки их две — каждая над своей половиной — и гаснут,
 * когда половина сжимается в полоску: подпись без картинки под ней читалась
 * бы как подпись к соседней.
 */
@Composable
private fun Captions(viewModel: ResultPreviewViewModel) {
    Box(Modifier.fillMaxSize().padding(CAPTION_INSET)) {
        if (viewModel.showsComparison) {
            AnimatedVisibility(
                visible = viewModel.comparePosition > CAPTION_FADE,
                modifier = Modifier.align(Alignment.TopStart),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SlimlyFrameCaption(viewModel.beforeCaption)
            }

            AnimatedVisibility(
                visible = viewModel.comparePosition < 1f - CAPTION_FADE,
                modifier = Modifier.align(Alignment.TopEnd),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SlimlyFrameCaption(viewModel.afterCaption, color = SlimlyColor.Accent200)
            }
        } else {
            SlimlyFrameCaption(
                text = viewModel.caption,
                modifier = Modifier.align(Alignment.TopStart),
                color = if (viewModel.isShowingAfter) SlimlyColor.Accent200 else SlimlyColor.Neutral300,
            )
        }
    }
}

/** Карточка веса: было, стало и насколько. */
@Composable
private fun Sizes(viewModel: ResultPreviewViewModel) {
    SlimlyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = viewModel.beforeText,
                style = SlimlyText.StatStruck,
                color = SlimlyColor.Neutral500,
                textDecoration = if (viewModel.showsDelta) TextDecoration.LineThrough else null,
            )

            SlimlyArrowRight(modifier = Modifier.padding(horizontal = SlimlySpacing.X3))

            Text(
                text = viewModel.afterText,
                style = SlimlyText.StatS,
                color = SlimlyColor.Accent300,
                modifier = Modifier.weight(1f),
            )

            if (viewModel.showsDelta) {
                SlimlyDeltaBadge(viewModel.deltaText)
            } else if (viewModel.outputFormat.isNotEmpty()) {
                SlimlyDeltaBadge(viewModel.outputFormat)
            }
        }
    }
}

private val CAPTION_INSET = 12.dp

/**
 * Доля окна, при которой подпись стороны уже не помещается над своей
 * половиной и гаснет.
 */
private const val CAPTION_FADE = 0.18f
