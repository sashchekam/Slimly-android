package com.slimly.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyClose
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyLinkButton
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap
import com.slimly.feature.onboarding.illustration.AudioIllustration
import com.slimly.feature.onboarding.illustration.CompressIllustration
import com.slimly.feature.onboarding.illustration.ConvertIllustration
import com.slimly.feature.onboarding.illustration.ScanIllustration

/**
 * Вводный тур: четыре инструмента приложения, по одному на шаг.
 *
 * Это корневой экран первого запуска, а не модалка поверх приложения:
 * возврата из него нет, и последний шаг меняет корень на главный.
 */
@Composable
internal fun OnboardingScreen(viewModel: OnboardingViewModel, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        OnboardingBackground()

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(viewModel)
            Content(viewModel, Modifier.weight(1f))
            Actions(viewModel)
        }
    }
}

/**
 * Шкала шагов и выход. Кнопки «назад» нет: шаг назад в туре никому не нужен,
 * а крестик ведёт туда же, куда и последний шаг.
 */
@Composable
private fun Header(viewModel: OnboardingViewModel) {
    val closeLabel = stringResource(R.string.tour_close)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TAP_TARGET)
            .padding(horizontal = SlimlySpacing.X6),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Steps(viewModel, Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(CLOSE_TARGET)
                .background(SlimlyColor.Text.copy(alpha = CLOSE_FILL_ALPHA), CircleShape)
                .slimlyTap(onClick = viewModel::skip)
                .semantics { contentDescription = closeLabel },
            contentAlignment = Alignment.Center,
        ) {
            SlimlyClose()
        }
    }
}

@Composable
private fun Steps(viewModel: OnboardingViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        viewModel.pages.indices.forEach { index ->
            val isCurrent = index == viewModel.pageIndex
            // Текущий шаг вытягивается в капсулу, а не меняет цвет: длина
            // читается краем глаза, а оттенок точки — нет.
            val width by animateDpAsState(
                targetValue = if (isCurrent) STEP_WIDE else STEP_NARROW,
                label = "step-width",
            )

            Box(
                Modifier
                    .width(width)
                    .height(STEP_HEIGHT)
                    .background(
                        color = if (isCurrent) SlimlyColor.Accent else SlimlyColor.Neutral800,
                        shape = RoundedCornerShape(STEP_HEIGHT),
                    ),
            )
        }
    }
}

@Composable
private fun Content(viewModel: OnboardingViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = CONTENT_HORIZONTAL),
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = viewModel.currentPage,
            transitionSpec = {
                val duration = TRANSITION_MILLIS
                (fadeIn(tween(duration)) + slideInVertically(tween(duration)) { STEP_SLIDE })
                    .togetherWith(fadeOut(tween(duration)))
            },
            label = "onboarding-page",
        ) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2)) {
                // Картинка живёт в блоке постоянной высоты: иллюстрации
                // разного размера иначе дёргали бы текст под собой.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ILLUSTRATION_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Illustration(page.illustration)
                }

                Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3)) {
                    Text(
                        text = stringResource(page.kicker).uppercase(),
                        style = SlimlyText.Kicker,
                        color = SlimlyColor.Accent300,
                    )
                    Text(text = stringResource(page.title), style = SlimlyText.Title, color = SlimlyColor.Text)
                    Text(text = stringResource(page.text), style = SlimlyText.Body, color = SlimlyColor.Neutral400)
                }
            }
        }
    }
}

@Composable
private fun Illustration(kind: OnboardingPage.Illustration) {
    when (kind) {
        OnboardingPage.Illustration.COMPRESS -> CompressIllustration()
        OnboardingPage.Illustration.CONVERT -> ConvertIllustration()
        OnboardingPage.Illustration.AUDIO -> AudioIllustration()
        OnboardingPage.Illustration.SCAN -> ScanIllustration()
    }
}

@Composable
private fun Actions(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = SlimlySpacing.ScreenHorizontal, vertical = SlimlySpacing.X2),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SlimlyButton(
            // На последнем шаге кнопка перестаёт вести дальше и становится
            // входом в приложение — иначе «Далее» упиралось бы в пустоту.
            title = stringResource(if (viewModel.isLastPage) R.string.tour_start else R.string.tour_next),
            onClick = viewModel::next,
        )

        SlimlyLinkButton(
            title = stringResource(R.string.tour_skip),
            onClick = viewModel::skip,
            color = SlimlyColor.Neutral500,
            modifier = Modifier.padding(vertical = SlimlySpacing.X2),
        )
    }
}

private val TAP_TARGET = 44.dp
private val CLOSE_TARGET = 30.dp
private val STEP_WIDE = 30.dp
private val STEP_NARROW = 14.dp
private val STEP_HEIGHT = 6.dp
private val CONTENT_HORIZONTAL = 24.dp
private val ILLUSTRATION_HEIGHT = 262.dp
private const val CLOSE_FILL_ALPHA = 0.09f
private const val TRANSITION_MILLIS = 450
private const val STEP_SLIDE = 24
