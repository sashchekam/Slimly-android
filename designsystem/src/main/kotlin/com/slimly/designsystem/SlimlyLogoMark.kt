package com.slimly.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp

/**
 * Знак Slimly: четыре квадрата ромбом — по одному на инструмент хаба.
 *
 * Три обведены контуром, четвёртый залит акцентом. Заливка здесь —
 * исключение из правила «акцент не заливает площади»: на иконке в 40 dp
 * контуры сливаются, и узнать приложение можно только по цветному пятну.
 *
 * `size` — сторона макетной сетки, как атрибут `width` у исходного SVG.
 * Повёрнутый знак выходит за неё примерно на 10 % с каждой стороны, поэтому
 * в тесной раскладке ему нужен запас.
 */
@Composable
fun SlimlyLogoMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val scale = this.size.width / SlimlyMarkGrid.BOX
        val line = with(SlimlyMarkGrid) { markLineWidth() }

        rotate(SlimlyMarkGrid.ROTATION) {
            SlimlyMarkGrid.CORNERS.forEachIndexed { index, corner ->
                if (index == 0) {
                    drawMarkCell(corner, scale, SlimlyColor.Accent, stroke = null)
                } else {
                    val color = if (index == SlimlyMarkGrid.LIT_CORNER) SlimlyColor.Text else SlimlyColor.MarkMuted
                    drawMarkCell(corner, scale, color, stroke = Stroke(line))
                }
            }
        }
    }
}

/**
 * Один квадрат сетки. Контур рисуется по середине линии, как и в SwiftUI,
 * поэтому внешний габарит обведённого квадрата шире стороны на толщину.
 */
internal fun DrawScope.drawMarkCell(
    corner: Offset,
    scale: Float,
    color: Color,
    stroke: Stroke?,
    side: Float = SlimlyMarkGrid.CELL,
    cornerRadius: Float = SlimlyMarkGrid.CORNER,
    stretch: Size = Size(1f, 1f),
) {
    val width = side * scale * stretch.width
    val height = side * scale * stretch.height
    val center = Offset(size.width / 2f + corner.x * scale, size.height / 2f + corner.y * scale)

    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - width / 2f, center.y - height / 2f),
        size = Size(width, height),
        cornerRadius = CornerRadius(cornerRadius * scale),
        style = stroke ?: Fill,
    )
}
