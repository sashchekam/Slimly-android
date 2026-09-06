package com.slimly.feature.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.QuadPoint
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyHaptic
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyStripeStyle
import com.slimly.designsystem.SlimlyStripes
import com.slimly.designsystem.rememberSlimlyHaptics
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Кадр страницы с рамкой, за углы которой можно тянуть.
 *
 * Всё вне рамки притушено — так видно не «где линии», а «что останется»:
 * от границ зависит вся страница, и судить о них по одному контуру трудно.
 * Под пальцем угол не видно, поэтому на время перетаскивания в свободном
 * углу кадра появляется лупа с тем же куском изображения.
 */
@Composable
fun PageBoundsEditor(
    image: ImageBitmap?,
    /**
     * Пропорции, пока кадра нет: пустое место должно занять ровно столько,
     * сколько потом займёт страница, иначе экран прыгает при загрузке.
     */
    fallbackAspectRatio: Float,
    quad: DocumentQuad,
    onMove: (DocumentQuad.Corner, QuadPoint) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ratio = image?.let { it.width.toFloat() / it.height } ?: fallbackAspectRatio
    var drag: Drag? by remember(image) { mutableStateOf(null) }
    var frame by remember { mutableStateOf(Size.Zero) }
    val current by rememberUpdatedState(quad)
    val move by rememberUpdatedState(onMove)

    // Угол мелкий, а палец его закрывает: без отдачи неясно, взялись ли
    // за угол вообще, — и рамку тянут вслепую.
    val haptics = rememberSlimlyHaptics()
    LaunchedEffect(drag != null) { haptics(SlimlyHaptic.SELECTION) }

    Box(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(SlimlyRadius.Lg))
            .onSizeChanged { frame = it.toSize() }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { drag = grab(current, it, size.toSize(), TOUCH.toPx()) },
                    onDragEnd = { drag = null },
                    onDragCancel = { drag = null },
                ) { change, _ ->
                    val held = drag ?: return@detectDragGestures
                    change.consume()
                    val point = change.position + held.grab
                    move(
                        held.corner,
                        QuadPoint(x = point.x / size.width, y = point.y / size.height),
                    )
                }
            },
    ) {
        Page(image)

        Canvas(Modifier.fillMaxSize()) {
            drawShade(current)
            drawOutline(current)
            drawHandles(current, drag?.corner)
        }

        val held = drag
        if (held != null && image != null) {
            Loupe(
                image = image,
                quad = current,
                corner = held.corner,
                frame = frame,
                modifier = Modifier
                    .align(if (current[held.corner].x < HALF) Alignment.TopEnd else Alignment.TopStart)
                    .padding(LOUPE_INSET),
            )
        }
    }
}

/** Угол под пальцем и промах, с которым за него взялись. */
private data class Drag(val corner: DocumentQuad.Corner, val grab: Offset)

/**
 * За какой угол взялись.
 *
 * Промах запоминаем: без него угол прыгнул бы под палец в первый же кадр
 * перетаскивания, хотя человек целился в кольцо ручки, а не в саму точку.
 */
private fun grab(quad: DocumentQuad, touch: Offset, size: Size, reach: Float): Drag? {
    val corner = DocumentQuad.Corner.entries.minByOrNull { point(quad[it], size).distanceTo(touch) } ?: return null
    val position = point(quad[corner], size)
    if (position.distanceTo(touch) > reach) return null
    return Drag(corner, position - touch)
}

@Composable
private fun Page(image: ImageBitmap?) {
    // Пока кадра нет, на его месте полосатая заглушка, а не пустота:
    // дыра в списке страниц читается как поломка.
    SlimlyStripes(Modifier.fillMaxSize(), SlimlyStripeStyle.TILE)

    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

// MARK: Части рамки

/** Всё, что вне рамки, гасим: страница остаётся яркой, фон уходит назад. */
private fun DrawScope.drawShade(quad: DocumentQuad) {
    val path = Path().apply {
        // Чётно-нечётное правило вырезает рамку из общей заливки — так
        // затемнение получается одной фигурой, а не четырьмя полосами.
        fillType = PathFillType.EvenOdd
        addRect(Rect(Offset.Zero, size))
        addPolygon(quad.corners.map { point(it, size) })
    }
    drawPath(path, SlimlyColor.Background.copy(alpha = SHADE))
}

/** Контур страницы: линия по сторонам и уголки поверх неё. */
private fun DrawScope.drawOutline(quad: DocumentQuad) {
    val points = quad.corners.map { point(it, size) }

    drawPath(Path().apply { addPolygon(points) }, SlimlyColor.Accent, style = Stroke(width = EDGE.toPx()))

    // Короткие усы у каждого угла — по ним видно, куда именно тянуть,
    // даже когда сама линия теряется на светлой странице.
    val whiskers = Path().apply {
        points.forEachIndexed { index, corner ->
            listOf(points.previous(index), points.next(index)).forEach { neighbour ->
                moveTo(corner.x, corner.y)
                val target = corner.approaching(neighbour, WHISKER.toPx())
                lineTo(target.x, target.y)
            }
        }
    }
    drawPath(
        whiskers,
        SlimlyColor.Accent200,
        style = Stroke(width = WHISKER_EDGE.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Кольца ручек. Тот, за который держат, крупнее: под пальцем его не видно. */
private fun DrawScope.drawHandles(quad: DocumentQuad, active: DocumentQuad.Corner?) {
    DocumentQuad.Corner.entries.forEach { corner ->
        val center = point(quad[corner], size)
        val radius = HANDLE.toPx() / 2 * if (corner == active) HANDLE_ACTIVE else 1f

        drawCircle(SlimlyColor.Accent.copy(alpha = HANDLE_FILL), radius = radius, center = center)
        drawCircle(
            SlimlyColor.Accent200,
            radius = radius,
            center = center,
            style = Stroke(width = HANDLE_EDGE.toPx()),
        )
        drawCircle(SlimlyColor.Accent200, radius = HANDLE_CORE.toPx() / 2, center = center)
    }
}

// MARK: Лупа

/**
 * Кусок кадра под пальцем, увеличенный в два с половиной раза.
 *
 * Сильнее нельзя: на увеличении крупнее трёх крат в круг перестаёт помещаться
 * край страницы, и становится не видно, куда относительно него едет угол.
 */
@Composable
private fun Loupe(
    image: ImageBitmap,
    quad: DocumentQuad,
    corner: DocumentQuad.Corner,
    /** Размер кадра под лупой — в нём считаются доли. */
    frame: Size,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(LOUPE)
            .clip(CircleShape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val zoomed = Size(frame.width * ZOOM, frame.height * ZOOM)
            val focus = point(quad[corner], zoomed)
            val shift = Offset(size.width / 2 - focus.x, size.height / 2 - focus.y)

            drawRect(SlimlyColor.SurfaceSunken)
            drawImage(
                image = image,
                dstOffset = IntOffset(shift.x.roundToInt(), shift.y.roundToInt()),
                dstSize = IntSize(zoomed.width.roundToInt(), zoomed.height.roundToInt()),
            )

            // Рамка внутри лупы: без неё увеличенный кусок бумаги ничем
            // не отличается от любого другого куска бумаги.
            drawPath(
                Path().apply { addPolygon(quad.corners.map { point(it, zoomed) + shift }) },
                SlimlyColor.Accent200,
                style = Stroke(width = EDGE.toPx()),
            )

            drawCrosshair()
            drawMark()
        }

        // Кромка вместо тени: круг лежит поверх кадра, и подсветить его
        // здесь можно только линией — как всё в Nocturne.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                SlimlyColor.Accent600,
                radius = size.minDimension / 2 - HAIR.toPx() / 2,
                style = Stroke(width = HAIR.toPx()),
            )
        }
    }
}

/**
 * Крест вокруг точки прицела разорван: сплошные линии перечеркнули бы
 * саму точку ровно там, где на неё и смотрят.
 */
private fun DrawScope.drawCrosshair() {
    val center = Offset(size.width / 2, size.height / 2)
    listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)).forEach { direction ->
        drawLine(
            color = SlimlyColor.Accent100.copy(alpha = CROSS_ALPHA),
            start = center + direction * MARK_GAP.toPx(),
            end = center + direction * MARK_REACH.toPx(),
            strokeWidth = HAIR.toPx(),
        )
    }
}

/**
 * Сама точка угла. Светлая обводка нужна на тёмной странице так же,
 * как заливка — на белой: без неё точка теряется в одном из двух случаев.
 */
private fun DrawScope.drawMark() {
    val center = Offset(size.width / 2, size.height / 2)
    drawCircle(SlimlyColor.Accent, radius = MARK.toPx() / 2, center = center)
    drawCircle(
        SlimlyColor.Accent100,
        radius = MARK.toPx() / 2,
        center = center,
        style = Stroke(width = MARK_EDGE.toPx()),
    )
}

// MARK: Мелочи геометрии

/** Доли кадра — в точки экрана. */
internal fun point(point: QuadPoint, size: Size): Offset = Offset(point.x * size.width, point.y * size.height)

private fun Path.addPolygon(points: List<Offset>) {
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

private fun Offset.distanceTo(other: Offset): Float = hypot(other.x - x, other.y - y)

/** Соседи угла по обходу рамки: рамка замкнута, поэтому индексы — по кругу. */
internal fun List<Offset>.previous(index: Int): Offset = this[(index + size - 1) % size]

internal fun List<Offset>.next(index: Int): Offset = this[(index + 1) % size]

/** Точка на отрезке к соседу: длиной в [distance], но не длиннее доли стороны. */
private fun Offset.approaching(other: Offset, distance: Float): Offset {
    val side = distanceTo(other)
    if (side <= 0f) return this
    val fraction = minOf(distance / side, WHISKER_SHARE)
    return Offset(x + (other.x - x) * fraction, y + (other.y - y) * fraction)
}

private const val HALF = 0.5f
private const val SHADE = 0.68f

private val EDGE = 1.5.dp
private val HAIR = 1.dp
private val WHISKER = 20.dp
private val WHISKER_EDGE = 3.dp
private const val WHISKER_SHARE = 0.28f

/**
 * Кольцо мелкое, чтобы не закрывать край страницы, а нажатие ловится
 * по всей сорокачетырёхточечной цели вокруг него.
 */
private val HANDLE = 26.dp
private val HANDLE_CORE = 5.dp
private val HANDLE_EDGE = 2.dp
private val TOUCH = 44.dp
private const val HANDLE_FILL = 0.22f
private const val HANDLE_ACTIVE = 1.3f

private val LOUPE = 104.dp
private val LOUPE_INSET = 10.dp
private const val ZOOM = 2.6f

private val MARK = 9.dp
private val MARK_EDGE = 1.5.dp
private val MARK_GAP = 8.dp
private val MARK_REACH = 17.dp
private const val CROSS_ALPHA = 0.9f
