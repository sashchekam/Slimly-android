package com.slimly.feature.onboarding.illustration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyText
import com.slimly.feature.onboarding.R

/** Дорожка колеблется, под ней проявляется, во что она превратится. */
@Composable
internal fun AudioIllustration(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BLOCK_GAP),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.height(WAVE_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
            verticalAlignment = Alignment.Bottom,
        ) {
            OFFSETS.forEach { offset -> WaveBar(phase = offset) }
        }

        ResultPill()
    }
}

/**
 * Сдвиг фазы каждого столбика. Ряд неровный намеренно: ровная волна читается
 * как индикатор загрузки, а не как звук.
 */
private val OFFSETS = listOf(
    0.22f, 0.55f, 0.9f, 0.4f, 0.72f, 0.15f, 0.62f, 0.95f, 0.3f,
    0.5f, 0.85f, 0.2f, 0.68f, 0.42f, 0.78f, 0.28f, 0.58f, 0.35f,
)

/** Столбик дорожки: качается между четвертью и полной высотой. */
@Composable
private fun WaveBar(phase: Float) {
    val transition = rememberInfiniteTransition(label = "wave")
    val height by transition.animateFloat(
        initialValue = BAR_LOW,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWING_MILLIS, delayMillis = (phase * SWING_MILLIS).toInt()),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wave-height",
    )

    Box(
        Modifier
            .width(BAR_WIDTH)
            .height(BAR_MAX * height)
            .background(
                Brush.verticalGradient(listOf(SlimlyColor.Accent200, SlimlyColor.Accent600)),
                RoundedCornerShape(SlimlyRadius.Pill),
            ),
    )
}

/** «MOV → M4A · 4,2 МБ»: во что превратится дорожка. */
@Composable
private fun ResultPill(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pill")
    val millis = { seconds: Float -> (seconds * SECOND).toInt() }

    val opacity by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                0f at millis(1.43f) using LinearEasing
                1f at millis(1.95f)
                1f at millis(2.39f) using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "pill-opacity",
    )

    val shape = RoundedCornerShape(SlimlyRadius.Pill)
    val mono = SlimlyText.CaptionSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)

    Row(
        modifier = modifier
            .alpha(opacity)
            .background(SlimlyColor.Surface, shape)
            .border(EDGE, SlimlyColor.Text.copy(alpha = EDGE_ALPHA), shape)
            .padding(horizontal = PILL_HORIZONTAL, vertical = PILL_VERTICAL),
        horizontalArrangement = Arrangement.spacedBy(PILL_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = SOURCE_FORMAT, style = mono, color = SlimlyColor.Neutral500)
        Box(
            Modifier
                .size(width = DASH_WIDTH, height = DASH_HEIGHT)
                .background(SlimlyColor.Neutral600),
        )
        Text(text = stringResource(R.string.audio_sample), style = mono, color = SlimlyColor.Accent200)
    }
}

private const val SOURCE_FORMAT = "MOV"

/** Круг — 2,6 с: волна успевает качнуться дважды, подпись — проявиться раз. */
private const val CYCLE = 2600
private const val SECOND = 1000f
private const val SWING_MILLIS = 1100
private const val BAR_LOW = 0.22f
private const val EDGE_ALPHA = 0.12f
private val EDGE = 1.dp
private val BAR_WIDTH = 5.dp
private val BAR_MAX = 70.dp
private val BAR_GAP = 4.dp
private val WAVE_HEIGHT = 78.dp
private val BLOCK_GAP = 20.dp
private val PILL_HORIZONTAL = 13.dp
private val PILL_VERTICAL = 8.dp
private val PILL_GAP = 9.dp
private val DASH_WIDTH = 14.dp
private val DASH_HEIGHT = 1.dp
