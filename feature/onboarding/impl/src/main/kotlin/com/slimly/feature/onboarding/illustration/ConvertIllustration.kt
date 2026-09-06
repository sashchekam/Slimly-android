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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slimly.designsystem.SlimlyArrowRight
import com.slimly.designsystem.SlimlyChip
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyText

/** Карточка меняет формат и превращается в готовый кадр, под ней — ряд форматов. */
@Composable
internal fun ConvertIllustration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "convert")
    val millis = { seconds: Float -> (seconds * SECOND).toInt() }

    // Круг закрывается обратным перекрёстком: без него подписи на карточке
    // щёлкали бы местами в момент повтора.
    val source by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                1f at millis(0.84f) using LinearEasing
                0f at millis(1.54f)
                0f at millis(2.52f) using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "convert-source",
    )

    val target by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                0f at millis(1.26f) using LinearEasing
                1f at millis(1.96f)
                1f at millis(2.52f) using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "convert-target",
    )

    val arrow by transition.animateFloat(
        initialValue = ARROW_DIM,
        targetValue = ARROW_DIM,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = CYCLE
                1f at millis(1.4f)
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "convert-arrow",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BLOCK_GAP),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CARD_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                FormatCard(format = SOURCE_FORMAT, isTarget = false, modifier = Modifier.alpha(source))
                FormatCard(format = TARGET_FORMAT, isTarget = true, modifier = Modifier.alpha(target))
            }

            SlimlyArrowRight(
                size = ARROW_SIZE,
                color = SlimlyColor.Accent300,
                modifier = Modifier
                    .alpha(arrow)
                    .scale(ARROW_SCALE_FROM + (1f - ARROW_SCALE_FROM) * arrow),
            )

            PaperSurface(
                Modifier
                    .size(width = CARD_WIDTH, height = CARD_HEIGHT)
                    .clip(RoundedCornerShape(CARD_RADIUS)),
            )
        }

        // Ряд под картинкой — те же чипы, что на шаге выбора формата: тур
        // показывает интерфейс, а не рисует свой.
        Row(horizontalArrangement = Arrangement.spacedBy(CHIP_GAP)) {
            FORMATS.forEach { format ->
                SlimlyChip(title = format, isSelected = format == TARGET_FORMAT, onClick = {})
            }
        }
    }
}

/** Карточка исходника: контур с названием формата вместо превью. */
@Composable
private fun FormatCard(format: String, isTarget: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(CARD_RADIUS)

    Box(
        modifier = modifier
            .size(width = CARD_WIDTH, height = CARD_HEIGHT)
            .background(
                // Формат, в который переводим, подсвечен акцентом —
                // исходник нейтрален.
                color = if (isTarget) SlimlyColor.Accent.copy(alpha = TARGET_FILL) else Color.Transparent,
                shape = shape,
            )
            .border(
                width = EDGE,
                color = if (isTarget) SlimlyColor.Accent else SlimlyColor.Text.copy(alpha = EDGE_ALPHA),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = format,
            style = SlimlyText.RowTitle.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
            color = if (isTarget) SlimlyColor.Accent100 else SlimlyColor.Neutral300,
        )
    }
}

private val FORMATS = listOf("HEIC", "JPEG", "PNG", "H.264")
private const val SOURCE_FORMAT = "HEIC"
private const val TARGET_FORMAT = "JPEG"

/** Круг — 2,8 с: полный перекрёсток «исходник → результат» и обратно. */
private const val CYCLE = 2800
private const val SECOND = 1000f
private const val ARROW_DIM = 0.35f
private const val ARROW_SCALE_FROM = 0.94f
private const val TARGET_FILL = 0.14f
private const val EDGE_ALPHA = 0.14f
private val EDGE = 1.dp
private val CARD_WIDTH = 74.dp
private val CARD_HEIGHT = 92.dp
private val CARD_RADIUS = 9.dp
private val CARD_GAP = 14.dp
private val CHIP_GAP = 7.dp
private val BLOCK_GAP = 18.dp
private val ARROW_SIZE = 26.dp
