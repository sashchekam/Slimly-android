package com.slimly.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Оживший знак: акцент обходит четыре квадрата против часовой стрелки
 * и на ходу вытягивается вдоль движения.
 *
 * Кадры взяты из макета (60 fps): перелёт на соседний угол идёт кривой
 * (0.55, 0, 0.2, 1), а растяжение 128/78 % приходится ровно на середину
 * перелёта — квадрат разгоняется и «долетает», а не тормозит рывком.
 * Круг здесь один: после него открывается приложение. В конце верхний контур
 * загорается белым — знак складывается в ту же композицию, что на иконке.
 *
 * В движении все четыре контура приглушённые: белый под летящим акцентом
 * давал бы второе яркое пятно и спорил с ним за внимание.
 */
@Composable
fun SlimlyLogoMarkAnimation(
    size: Dp,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
    onLanding: () -> Unit = {},
    onFinish: () -> Unit = {},
) {
    // Положение считается дробным номером угла, а не парой координат: между
    // соседними углами оно просто интерполируется, и перелёт описывается
    // одним числом.
    val position = remember { Animatable(0f) }
    val stretchX = remember { Animatable(1f) }
    val stretchY = remember { Animatable(1f) }
    val settled = remember { Animatable(0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            settled.snapTo(1f)
            delay(STILL_MILLIS)
            onFinish()
            return@LaunchedEffect
        }

        // Пауза перед первым перелётом: знак стоит ровно в том же кадре, что
        // и на экране запуска, — стык системной картинки и заставки не виден.
        delay(LEAD_IN_MILLIS)

        repeat(SlimlyMarkGrid.CORNERS.size) { step ->
            // Перелёты чередуют оси, поэтому растяжение — то по ширине,
            // то по высоте: тянет всегда вдоль движения.
            val alongX = step % 2 == 0
            val peakX = if (alongX) STRETCH else SQUEEZE
            val peakY = if (alongX) SQUEEZE else STRETCH

            launch { position.animateTo(step + 1f, tween(HOP_MILLIS, easing = HOP_EASING)) }
            launch { stretchX.animateTo(peakX, tween(HOP_MILLIS / 2, easing = HOP_EASING)) }
            stretchY.animateTo(peakY, tween(HOP_MILLIS / 2, easing = HOP_EASING))

            launch { stretchX.animateTo(1f, tween(HOP_MILLIS / 2, easing = HOP_EASING)) }
            stretchY.animateTo(1f, tween(HOP_MILLIS / 2, easing = HOP_EASING))

            onLanding()
            delay(GAP_MILLIS)
        }

        settled.animateTo(1f, tween(SETTLE_MILLIS))
        onFinish()
    }

    Canvas(modifier.size(size)) {
        val scale = this.size.width / SlimlyMarkGrid.BOX
        val line = with(SlimlyMarkGrid) { markLineWidth() }
        val corners = SlimlyMarkGrid.CORNERS

        rotate(SlimlyMarkGrid.ROTATION) {
            corners.forEachIndexed { index, corner ->
                val isLit = index == SlimlyMarkGrid.LIT_CORNER
                drawMarkCell(
                    corner = corner,
                    scale = scale,
                    color = if (isLit) {
                        lerp(SlimlyColor.MarkMuted, SlimlyColor.Text, settled.value)
                    } else {
                        SlimlyColor.MarkMuted
                    },
                    stroke = Stroke(line),
                )
            }

            drawMarkCell(
                corner = accentOffset(position.value, corners),
                scale = scale,
                color = SlimlyColor.Accent,
                stroke = null,
                side = SlimlyMarkGrid.ACCENT_CELL,
                cornerRadius = SlimlyMarkGrid.ACCENT_CORNER,
                stretch = Size(stretchX.value, stretchY.value),
            )
        }
    }
}

/**
 * Где сейчас акцент. Номер растёт до четырёх, а не сбрасывается в ноль:
 * иначе последний перелёт отыгрался бы задом наперёд, через весь знак.
 */
private fun accentOffset(position: Float, corners: List<Offset>): Offset {
    val from = corners[position.toInt() % corners.size]
    val to = corners[(position.toInt() + 1) % corners.size]
    val fraction = position - position.toInt()
    return Offset(
        x = from.x + (to.x - from.x) * fraction,
        y = from.y + (to.y - from.y) * fraction,
    )
}

/**
 * Тайминги круга. Собраны в одном месте: длительность заставки — это ровно
 * их сумма, и подбирать её приходится целиком.
 */
private const val LEAD_IN_MILLIS = 360L
private const val HOP_MILLIS = 320
private const val GAP_MILLIS = 40L
private const val SETTLE_MILLIS = 220

/** Сколько знак стоит, если система просит меньше движения. */
private const val STILL_MILLIS = 700L

/** Растяжение вдоль оси перелёта на его середине. */
private const val STRETCH = 1.28f
private const val SQUEEZE = 0.78f

private val HOP_EASING = CubicBezierEasing(0.55f, 0f, 0.2f, 1f)
