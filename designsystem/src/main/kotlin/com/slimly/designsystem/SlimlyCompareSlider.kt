package com.slimly.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Шторка сравнения «до / после»: кадры лежат стопкой в одном окне, а сколько
 * видно верхнего, решает вертикальная линия, которую таскают пальцем.
 * Изначально линия стоит посередине.
 *
 * Почему не переключатель на два положения: разница между оригиналом
 * и сжатой копией держится в мелочах — зерне неба, кромке волос, — и,
 * подменяя кадр целиком, её приходится держать в памяти. Со шторкой обе
 * стороны стоят рядом, и стык подводят прямо к спорному месту.
 *
 * Линию можно тащить откуда угодно, а не только за ручку, как на iOS: там
 * жест пришлось запирать в узкую полосу, иначе он отбирал прокрутку у экрана
 * под собой. Здесь тащить умеет отдельный распознаватель горизонтального
 * смаха — вертикальную прокрутку он не трогает, и запирать нечего.
 */
@Composable
fun SlimlyCompareSlider(
    position: Float,
    modifier: Modifier = Modifier,
    onPosition: (Float) -> Unit,
    before: @Composable () -> Unit,
    after: @Composable () -> Unit,
) {
    val fraction = position.coerceIn(0f, 1f)
    var width by remember { mutableIntStateOf(0) }
    val knob = with(LocalDensity.current) { KNOB.toPx() }
    val label = stringResource(R.string.accessibility_compare)
    val percent = (fraction * PERCENT).roundToInt()

    Box(
        modifier
            .onSizeChanged { width = it.width }
            .pointerInput(Unit) {
                // Линия ходит и по нажатию: тащить её через весь кадр ради
                // дальнего края — работа.
                detectTapGestures { point -> onPosition(share(point.x, size.width)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> onPosition(share(change.position.x, size.width)) }
            }
            .semantics {
                contentDescription = label
                stateDescription = "$percent%"
            },
    ) {
        // Обе стороны меряются окном, а не собой: кадр крупнее окна оставил бы
        // маску шторки считаться от своих краёв — линия разъезжалась бы
        // с картинкой под ней.
        Box(Modifier.matchParentSize()) { after() }

        Box(
            Modifier
                .matchParentSize()
                // Обрезка при отрисовке, а не рамка по ширине: кадр должен
                // остаться на своём месте и уйти под линию, а не сжаться
                // в оставшуюся полосу.
                .drawWithContent {
                    clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
                },
        ) {
            before()
        }

        Canvas(Modifier.matchParentSize()) {
            val x = size.width * fraction
            // Тёмная подложка под линией: на светлом кадре белая линия
            // растворяется, и шторка перестаёт читаться как край.
            drawLine(SHADE, Offset(x, 0f), Offset(x, size.height), (LINE + SHADE_EDGE * 2).toPx())
            drawLine(SlimlyColor.Neutral100, Offset(x, 0f), Offset(x, size.height), LINE.toPx())
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(x = (width * fraction - knob / 2).roundToInt(), y = 0) }
                .shadow(ELEVATION, CircleShape)
                .size(KNOB)
                .background(SlimlyColor.Neutral100, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Стрелки врозь, а не встык: сдвинутые вплотную «‹›» читаются
            // как «свернуть», а не как «тяни в обе стороны».
            Row(horizontalArrangement = Arrangement.spacedBy(ARROW_GAP)) {
                SlimlyChevronLeft(size = ARROW, color = SlimlyColor.Background)
                SlimlyChevronRight(size = ARROW, color = SlimlyColor.Background)
            }
        }
    }
}

private fun share(x: Float, width: Int): Float = if (width > 0) (x / width).coerceIn(0f, 1f) else HALF

private val KNOB = 34.dp
private val LINE = 2.dp
private val SHADE_EDGE = 1.dp
private val ELEVATION = 6.dp
private val ARROW = 6.dp
private val ARROW_GAP = 6.dp
private val SHADE = Color.Black.copy(alpha = 0.45f)
private const val HALF = 0.5f
private const val PERCENT = 100
