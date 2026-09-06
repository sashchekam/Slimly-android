package com.slimly.feature.onboarding.illustration

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyStripeStyle
import com.slimly.designsystem.SlimlyStripes
import com.slimly.designsystem.SlimlyText
import com.slimly.feature.onboarding.R

/**
 * Три файла ужимаются один за другим, под ними проявляется итог.
 *
 * Полосы уходят до трети длины: цифра «−58 %» под ними должна читаться как
 * подпись к тому, что уже видно, а не как отдельное утверждение.
 */
@Composable
internal fun CompressIllustration(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SHELF_GAP)) {
        Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
            ROWS.forEach { row ->
                ShrinkRow(delay = row.delay, modifier = Modifier.alpha(row.opacity))
            }
        }

        // Подпись выровнена не по краю картинки, а по началу полос: она
        // относится к ним, а не ко всему блоку.
        SavingLabel(Modifier.padding(start = LABEL_INDENT))
    }
}

/**
 * Задержки строк. Полосы стартуют вместе, но ужимаются по очереди — так
 * видно, что файлов несколько, а не один длинный.
 */
private data class ShrinkPhase(val delay: Float, val opacity: Float)

private val ROWS = listOf(
    ShrinkPhase(delay = 0f, opacity = 1f),
    ShrinkPhase(delay = 0.18f, opacity = 0.6f),
    ShrinkPhase(delay = 0.36f, opacity = 0.32f),
)

/** Миниатюра файла и полоса его размера. */
@Composable
private fun ShrinkRow(delay: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shrink")
    val millis = { seconds: Float -> (seconds * SECOND).toInt() }

    val width by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                1f at millis(0.5f + delay) using LinearEasing
                0.34f at millis(1.85f + delay)
                // Круг у всех строк одной длины: задержка съедается хвостом,
                // иначе полосы за минуту расходятся и стая распадается.
                0.34f at millis(2.8f) using LinearEasing
                // Ширина возвращается в единицу, пока строка уже невидима.
                1f at millis(2.81f) using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "shrink-width",
    )

    val opacity by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                1f at millis(2.5f) using LinearEasing
                0f at millis(2.8f)
                0f at millis(2.81f) using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "shrink-opacity",
    )

    Row(
        modifier = modifier.alpha(opacity),
        horizontalArrangement = Arrangement.spacedBy(THUMB_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SlimlyStripes(
            modifier = Modifier
                .size(THUMB)
                .clip(RoundedCornerShape(THUMB_RADIUS)),
            style = SlimlyStripeStyle.THUMBNAIL,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .background(SlimlyColor.Text.copy(alpha = BAR_TRACK_ALPHA), RoundedCornerShape(SlimlyRadius.Pill)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val target = (constraints.maxWidth * width).toInt()
                        val placeable = measurable.measure(constraints.copy(minWidth = target, maxWidth = target))
                        layout(target, placeable.height) { placeable.place(0, 0) }
                    }
                    .background(
                        Brush.horizontalGradient(listOf(SlimlyColor.Accent, SlimlyColor.Accent300)),
                        RoundedCornerShape(SlimlyRadius.Pill),
                    ),
            )
        }
    }
}

/**
 * «−58% · 1,4 ГБ → 590 МБ» — появляется, когда полосы уже ужались, и гаснет
 * вместе с ними.
 */
@Composable
private fun SavingLabel(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "saving")
    val millis = { seconds: Float -> (seconds * SECOND).toInt() }

    val opacity by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                0f at millis(1.4f) using LinearEasing
                1f at millis(1.9f)
                1f at millis(2.5f) using LinearEasing
                0f at millis(2.8f)
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "saving-opacity",
    )

    Row(
        modifier = modifier.alpha(opacity),
        horizontalArrangement = Arrangement.spacedBy(LABEL_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = SAVING, style = SlimlyText.Title, color = SlimlyColor.Accent200)
        Text(
            text = stringResource(R.string.compress_sample),
            style = SlimlyText.BodySmall,
            color = SlimlyColor.Neutral400,
            modifier = Modifier.padding(bottom = BASELINE),
        )
    }
}

/** Круг анимации — 2,9 с: столько нужно, чтобы взгляд успел за все три строки. */
private const val CYCLE = 2900
private const val SECOND = 1000f
private const val SAVING = "−58%"
private const val BAR_TRACK_ALPHA = 0.09f
private val SHELF_GAP = 13.dp
private val ROW_GAP = 9.dp
private val THUMB = 34.dp
private val THUMB_GAP = 10.dp
private val THUMB_RADIUS = 7.dp
private val BAR_HEIGHT = 9.dp
private val LABEL_INDENT = 44.dp
private val LABEL_GAP = 9.dp
private val BASELINE = 2.dp
