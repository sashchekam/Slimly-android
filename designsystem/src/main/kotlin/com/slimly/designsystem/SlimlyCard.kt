package com.slimly.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Карточка-поверхность: заливка, скругление и волосяная кромка.
 *
 * Высота на тёмном фоне — это кромка и темнота вокруг, а не мягкая тень:
 * залитый тенью элемент «всплывает» и ломает плоскую глубину системы.
 */
@Composable
fun SlimlyCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = CardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(SlimlyRadius.Lg)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SlimlyColor.Surface)
            .border(width = EDGE, color = SlimlyColor.Neutral800, shape = shape)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Сгруппированный список: строки лежат на поверхности, между ними просвет
 * в один пиксель, сквозь который виден полупрозрачный фон группы. Так
 * разделители получаются сплошными внутри контрола, а рисовать их отдельными
 * линейками не приходится.
 */
@Composable
fun SlimlyRowGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(SlimlyRadius.Lg)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SlimlyColor.Text.copy(alpha = DIVIDER_ALPHA))
            .border(width = EDGE, color = SlimlyColor.Neutral800, shape = shape),
        verticalArrangement = Arrangement.spacedBy(EDGE),
        content = content,
    )
}

/**
 * Заливка строки внутри [SlimlyRowGroup].
 *
 * Цвет меняют там, где строка ещё и выбрана: тогда она уходит на ступень
 * глубже поверхности, а не красится акцентом — в списке из десяти строк
 * акцентная заливка перевесила бы сам список.
 */
fun Modifier.slimlyRow(fill: Color = SlimlyColor.Surface): Modifier = this
    .fillMaxWidth()
    .background(fill)
    .padding(horizontal = ROW_HORIZONTAL, vertical = ROW_VERTICAL)

/**
 * Ячейка внутри карточки: на ступень темнее поверхности, с волосяной кромкой.
 *
 * Так вложенный блок читается как часть карточки, а не как дырка в ней —
 * поэтому у него своя заливка, а не прозрачность.
 */
fun Modifier.slimlyInsetTile(): Modifier = composed {
    val shape = RoundedCornerShape(SlimlyRadius.Md)
    this
        .fillMaxWidth()
        .clip(shape)
        .background(SlimlyColor.Background)
        .border(width = EDGE, color = SlimlyColor.Text.copy(alpha = TILE_EDGE_ALPHA), shape = shape)
}

private val CardPadding = PaddingValues(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 16.dp)
private val EDGE = 1.dp
private val ROW_HORIZONTAL = 13.dp
private val ROW_VERTICAL = 12.dp
private const val DIVIDER_ALPHA = 0.07f
private const val TILE_EDGE_ALPHA = 0.08f
