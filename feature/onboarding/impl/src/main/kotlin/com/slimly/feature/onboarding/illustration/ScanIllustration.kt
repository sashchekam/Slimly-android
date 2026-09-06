package com.slimly.feature.onboarding.illustration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius

/** Страница в рамке, по ней сверху вниз идёт линия сканера. */
@Composable
internal fun ScanIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan")
    val millis = { seconds: Float -> (seconds * SECOND).toInt() }

    val position by transition.animateFloat(
        initialValue = LINE_START,
        targetValue = LINE_END,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                LINE_END at CYCLE using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan-position",
    )

    val opacity by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                1f at millis(0.26f) using LinearEasing
                1f at millis(1.93f) using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan-opacity",
    )

    Box(
        modifier = modifier
            .size(width = PAGE_WIDTH, height = PAGE_HEIGHT)
            // Рамка идёт по внешнему краю листа, а не по нему самому: так
            // видно, что границы уже найдены, а не дорисованы поверх.
            .border(FRAME, SlimlyColor.Accent, RoundedCornerShape(FRAME_RADIUS))
            .padding(FRAME_INSET)
            .clip(RoundedCornerShape(SlimlyRadius.Sm)),
        contentAlignment = Alignment.TopStart,
    ) {
        Page()

        Box(
            Modifier
                .offset(y = (PAGE_HEIGHT - FRAME_INSET * 2) * position)
                .fillMaxWidth()
                .height(LINE_HEIGHT)
                .alpha(opacity)
                .background(SlimlyColor.Accent),
        )
    }
}

@Composable
private fun Page() {
    PaperSurface(
        Modifier
            .size(width = PAGE_WIDTH - FRAME_INSET * 2, height = PAGE_HEIGHT - FRAME_INSET * 2),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = PAGE_HORIZONTAL, vertical = PAGE_VERTICAL),
            verticalArrangement = Arrangement.spacedBy(LINE_GAP),
        ) {
            TextLine(width = 0.58f, height = HEADING_HEIGHT, isHeading = true)
            TextLine(width = 1f, height = BODY_HEIGHT, isHeading = false)
            TextLine(width = 0.92f, height = BODY_HEIGHT, isHeading = false)
            TextLine(width = 0.96f, height = BODY_HEIGHT, isHeading = false)
            TextLine(width = 0.64f, height = BODY_HEIGHT, isHeading = false)
        }
    }
}

/**
 * Строка текста на листе. Заголовок темнее — иначе страница читается как
 * ровная штриховка, а не как документ.
 */
@Composable
private fun TextLine(width: Float, height: Dp, isHeading: Boolean) {
    Box(
        Modifier
            .fillMaxWidth(width)
            .height(height)
            .background(
                color = SlimlyColor.Background.copy(alpha = if (isHeading) HEADING_ALPHA else BODY_ALPHA),
                shape = RoundedCornerShape(TEXT_RADIUS),
            ),
    )
}

/** Круг — 2,2 с: столько идёт линия сканера от края до края листа. */
private const val CYCLE = 2200
private const val SECOND = 1000f
private const val LINE_START = 0.06f
private const val LINE_END = 0.92f
private const val HEADING_ALPHA = 0.42f
private const val BODY_ALPHA = 0.2f
private val PAGE_WIDTH = 164.dp
private val PAGE_HEIGHT = 216.dp
private val PAGE_HORIZONTAL = 18.dp
private val PAGE_VERTICAL = 22.dp
private val FRAME = 2.dp
private val FRAME_RADIUS = 6.dp
private val FRAME_INSET = 3.dp
private val LINE_HEIGHT = 2.dp
private val LINE_GAP = 8.dp
private val HEADING_HEIGHT = 8.dp
private val BODY_HEIGHT = 4.dp
private val TEXT_RADIUS = 2.dp
