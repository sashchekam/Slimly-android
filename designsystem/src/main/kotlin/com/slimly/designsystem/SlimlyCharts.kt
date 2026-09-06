package com.slimly.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Чем красить показатель. Акцент достаётся ведущему: если подсветить все,
 * подсветка перестаёт что-либо значить.
 */
enum class SlimlyChartTint {
    ACCENT,
    NEUTRAL,
}

/**
 * Искра: ряд столбиков одинаковой ширины, высота — доля от самого большого.
 *
 * Осей и подписей нет намеренно. Искра отвечает на «ровно или рывками»,
 * а на «сколько именно» отвечает цифра рядом — две шкалы на одном месте
 * спорили бы друг с другом.
 */
@Composable
fun SlimlySparkline(
    values: List<Double>,
    modifier: Modifier = Modifier,
    tint: SlimlyChartTint = SlimlyChartTint.NEUTRAL,
    height: Dp = SPARK_HEIGHT,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            // Рисунок декоративный: то же самое сказано числами в той же
            // карточке, и читать вслух ряд столбиков незачем.
            .semantics { hideFromAccessibility() },
        horizontalArrangement = Arrangement.spacedBy(SPARK_GAP),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { index, value ->
            val fraction = value.coerceIn(0.0, 1.0).toFloat()
            Box(
                Modifier
                    .weight(1f)
                    // Пустой отрезок всё равно рисует полоску в два пункта:
                    // без неё в ряду появляется дыра, и он читается как обрыв
                    // данных, а не как ноль.
                    .height(maxOf(SPARK_FLOOR, height * fraction))
                    .background(
                        color = sparkColor(tint, isLast = index == values.lastIndex),
                        shape = RoundedCornerShape(SPARK_RADIUS),
                    ),
            )
        }
    }
}

/** Свежий столбик светлее остальных — он и есть «сейчас». */
private fun sparkColor(tint: SlimlyChartTint, isLast: Boolean): Color = when {
    tint == SlimlyChartTint.ACCENT && isLast -> SlimlyColor.Accent400
    tint == SlimlyChartTint.ACCENT -> SlimlyColor.Accent700
    isLast -> SlimlyColor.Neutral500
    else -> SlimlyColor.Neutral800
}

/** Кусок полосы долей: сколько занимает и каким цветом. */
data class SlimlyShare(val fraction: Double, val color: Color)

/**
 * Полоса долей: несколько кусков разного цвета в одной капсуле.
 *
 * Заменяет круговую диаграмму: доли здесь читают по длине, а не по углу,
 * и полоса ложится в ширину карточки, ничего не занимая по высоте.
 */
@Composable
fun SlimlyShareBar(shares: List<SlimlyShare>, modifier: Modifier = Modifier, height: Dp = SHARE_HEIGHT) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(SlimlyRadius.Pill))
            .background(SlimlyColor.Neutral900)
            .semantics { hideFromAccessibility() },
    ) {
        shares.forEach { share ->
            val fraction = share.fraction.coerceIn(0.0, 1.0).toFloat()
            Box(
                Modifier
                    .fillMaxHeight()
                    // Доля меряется от полосы целиком, поэтому ширина считается
                    // от ограничений, а не раздаётся весами: у весов пустой кусок
                    // всё равно получил бы свою часть остатка.
                    .layout { measurable, constraints ->
                        val width = (constraints.maxWidth * fraction).toInt()
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    }
                    .background(share.color),
            )
        }
    }
}

private val SPARK_HEIGHT = 34.dp
private val SPARK_GAP = 3.dp
private val SPARK_FLOOR = 2.dp
private val SPARK_RADIUS = 2.dp
private val SHARE_HEIGHT = 8.dp
