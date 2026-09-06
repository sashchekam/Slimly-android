package com.slimly.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Кольцо прогресса на экране обработки: акцентная дуга по тёмному ободу
 * и утопленный центр с процентом.
 */
@Composable
fun SlimlyProgressRing(value: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier.size(SlimlySize.ProgressRing),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val width = SlimlySize.ProgressRingWidth.toPx()
            val inset = width / 2f
            val arcSize = Size(size.width - width, size.height - width)

            drawCircle(color = SlimlyColor.Neutral900, radius = (size.minDimension - width) / 2f, style = Stroke(width))
            drawArc(
                color = SlimlyColor.Accent,
                // От двенадцати часов по часовой: кольцо читается как часы,
                // а не как произвольная дуга.
                startAngle = -90f,
                sweepAngle = 360f * value.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width, cap = StrokeCap.Round),
            )
        }

        content()
    }
}

/**
 * Чем залита полоса. Приглушённая достаётся тем показателям, которые стоят
 * рядом с ведущим: акцент в списке из четырёх полос должен доставаться
 * одной, иначе он перестаёт что-либо выделять.
 */
enum class SlimlyBarFill {
    SOLID,
    MUTED,
}

/**
 * Горизонтальная полоса прогресса. Толстые полосы-итоги заливаются
 * градиентом рампы, тонкие строчные — сплошным акцентом: на трёх пунктах
 * высоты градиент всё равно не читается.
 */
@Composable
fun SlimlyProgressBar(
    value: Float,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    fill: SlimlyBarFill = SlimlyBarFill.SOLID,
) {
    val shape = RoundedCornerShape(SlimlyRadius.Pill)
    val fraction = value.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(SlimlyColor.Neutral900, shape),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Ширина долей, а не `fillMaxWidth(fraction)`: нулевая доля
                // у последнего оставляет полосе минимальную видимую ширину.
                .layout { measurable, constraints ->
                    val width = (constraints.maxWidth * fraction).toInt()
                    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                    layout(width, placeable.height) { placeable.place(0, 0) }
                }
                .background(brush(fill), shape),
        )
    }
}

private fun brush(fill: SlimlyBarFill): Brush = when (fill) {
    SlimlyBarFill.SOLID -> Brush.horizontalGradient(listOf(SlimlyColor.Accent500, SlimlyColor.Accent600))
    SlimlyBarFill.MUTED -> SolidColor(SlimlyColor.Neutral700)
}
