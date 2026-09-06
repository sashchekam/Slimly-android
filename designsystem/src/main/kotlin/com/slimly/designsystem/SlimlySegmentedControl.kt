package com.slimly.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Переключатель на несколько положений: «до / после», разрез статистики.
 *
 * Выделение — одна пилюля, переезжающая между положениями, а не две разные,
 * гаснущие и загорающиеся: переезд показывает, что положение сменилось,
 * а не что экран перерисовали.
 */
@Composable
fun <T> SlimlySegmentedControl(
    options: List<T>,
    selected: T,
    title: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    if (options.isEmpty()) return

    // Доля, а не индекс: между положениями пилюля стоит посередине,
    // и целое число описывало бы только концы переезда.
    val offset by animateFloatAsState(
        targetValue = options.indexOf(selected).coerceAtLeast(0).toFloat(),
        // Пружина без раскачки: пилюля догоняет палец, но не проскакивает
        // мимо края переключателя.
        animationSpec = spring(dampingRatio = DAMPING, stiffness = Spring.StiffnessMediumLow),
        label = "segment",
    )

    Box(
        modifier = modifier
            .background(SlimlyColor.Text.copy(alpha = TRACK_ALPHA), RoundedCornerShape(SlimlyRadius.Pill))
            .padding(INSET),
    ) {
        // Пилюля лежит слоем ниже надписей: на переезде она перехватывала бы
        // нажатия соседнего положения.
        Highlight(count = options.size, offset = offset, modifier = Modifier.matchParentSize())

        Row(Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val isSelected = option == selected

                Text(
                    text = title(option),
                    style = SlimlyText.BodySmall,
                    color = if (isSelected) SlimlyColor.Accent200 else SlimlyColor.Neutral400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { this.selected = isSelected }
                        // Нажимается вся доля, а не надпись: у пилюли скруглены
                        // торцы, и по её форме углы сегмента выпадали бы
                        // из зоны нажатия.
                        .slimlyTap { onSelect(option) }
                        .padding(vertical = VERTICAL),
                )
            }
        }
    }
}

/**
 * Пилюля выделения шириной в одно положение, сдвинутая на [offset] положений
 * от левого края.
 *
 * Сдвиг делает своя раскладка, а не отступ: узел остаётся во всю ширину
 * переключателя, и вложенный в него фон — ровно на своей доле.
 */
@Composable
private fun Highlight(count: Int, offset: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .layout { measurable, constraints ->
                val width = constraints.maxWidth / count
                val placeable = measurable.measure(Constraints.fixed(width, constraints.maxHeight))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeRelative(x = (width * offset).roundToInt(), y = 0)
                }
            }
            .background(SlimlyColor.Accent.copy(alpha = HIGHLIGHT_ALPHA), RoundedCornerShape(SlimlyRadius.Pill)),
    )
}

private val INSET = 4.dp
private val VERTICAL = 9.dp
private const val TRACK_ALPHA = 0.07f
private const val HIGHLIGHT_ALPHA = 0.18f
private const val DAMPING = 0.85f
