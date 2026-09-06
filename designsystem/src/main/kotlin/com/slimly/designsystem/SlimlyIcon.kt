package com.slimly.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Иконки рисуются линией, а не приезжают ресурсом.
 *
 * Все они — открытые ломаные одной толщины: заводить ради этого набор
 * векторных файлов и следить, чтобы они не разъехались по толщине, дороже,
 * чем нарисовать.
 */
@Composable
fun SlimlyChevronLeft(modifier: Modifier = Modifier, size: Dp = 12.dp, color: Color = SlimlyColor.Accent300) {
    Canvas(modifier.size(width = size, height = size * CHEVRON_RATIO)) {
        val path = Path().apply {
            moveTo(this@Canvas.size.width, 0f)
            lineTo(0f, this@Canvas.size.height / 2f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height)
        }
        drawPath(path, color, style = stroke())
    }
}

/** Шеврон вправо: конец строки списка, ссылка «дальше». */
@Composable
fun SlimlyChevronRight(modifier: Modifier = Modifier, size: Dp = 6.dp, color: Color = SlimlyColor.Neutral600) {
    Canvas(modifier.size(width = size, height = size * CHEVRON_RATIO)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height / 2f)
            lineTo(0f, this@Canvas.size.height)
        }
        drawPath(path, color, style = stroke())
    }
}

/** Галочка выбранной плитки — на акцентном кружке. */
@Composable
fun SlimlyCheck(modifier: Modifier = Modifier, size: Dp = 11.dp, color: Color = SlimlyColor.OnAccent) {
    Canvas(modifier.size(width = size, height = size * CHECK_RATIO)) {
        val path = Path().apply {
            moveTo(0f, this@Canvas.size.height * CHECK_TURN)
            lineTo(this@Canvas.size.width * CHECK_CORNER, this@Canvas.size.height)
            lineTo(this@Canvas.size.width, 0f)
        }
        drawPath(path, color, style = stroke())
    }
}

/** Крестик: выход из тура и закрытие модалки. */
@Composable
fun SlimlyClose(modifier: Modifier = Modifier, size: Dp = 12.dp, color: Color = SlimlyColor.Neutral300) {
    Canvas(modifier.size(size)) {
        val side = this.size.width
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(side, side)
            moveTo(side, 0f)
            lineTo(0f, side)
        }
        drawPath(path, color, style = stroke())
    }
}

/** Стрелка «из → в» между форматами. */
@Composable
fun SlimlyArrowRight(modifier: Modifier = Modifier, size: Dp = 18.dp, color: Color = SlimlyColor.Neutral600) {
    Canvas(modifier.size(width = size, height = size * ARROW_RATIO)) {
        val width = this.size.width
        val height = this.size.height
        val path = Path().apply {
            moveTo(0f, height / 2f)
            lineTo(width, height / 2f)
            moveTo(width - height * ARROW_HEAD, 0f)
            lineTo(width, height / 2f)
            lineTo(width - height * ARROW_HEAD, height)
        }
        drawPath(path, color, style = stroke())
    }
}

/**
 * Камера: корпус с выступом видоискателя и объектив.
 *
 * Единственная иконка с окружностью внутри контура — объектив узнают
 * по ней, а не по форме корпуса.
 */
@Composable
fun SlimlyCamera(modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = SlimlyColor.Neutral300) {
    Canvas(modifier.size(width = size, height = size * CAMERA_RATIO)) {
        val width = this.size.width
        val height = this.size.height
        val corner = CornerRadius(width * CAMERA_CORNER)
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, height * CAMERA_BODY_TOP, width, height),
                    topLeft = corner,
                    topRight = corner,
                    bottomRight = corner,
                    bottomLeft = corner,
                ),
            )
            // Выступ рисуем трапецией, а не вторым прямоугольником: у корпуса
            // скруглённые углы, и приставленная сверху коробка топорщилась бы.
            moveTo(width * CAMERA_BUMP_INNER_LEFT, height * CAMERA_BODY_TOP)
            lineTo(width * CAMERA_BUMP_OUTER_LEFT, 0f)
            lineTo(width * CAMERA_BUMP_OUTER_RIGHT, 0f)
            lineTo(width * CAMERA_BUMP_INNER_RIGHT, height * CAMERA_BODY_TOP)
            addOval(
                Rect(
                    center = Offset(width / 2f, height * CAMERA_LENS_CENTER),
                    radius = width * CAMERA_LENS_RADIUS,
                ),
            )
        }
        drawPath(path, color, style = stroke())
    }
}

/** Галерея: рамка кадра с горами внутри — знак «снимок», а не «файл». */
@Composable
fun SlimlyGallery(modifier: Modifier = Modifier, size: Dp = 24.dp, color: Color = SlimlyColor.Neutral300) {
    Canvas(modifier.size(width = size, height = size * GALLERY_RATIO)) {
        val width = this.size.width
        val height = this.size.height
        val corner = CornerRadius(width * GALLERY_CORNER)
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(0f, 0f, width, height),
                    topLeft = corner,
                    topRight = corner,
                    bottomRight = corner,
                    bottomLeft = corner,
                ),
            )
            moveTo(width * GALLERY_START, height * GALLERY_BASE)
            lineTo(width * GALLERY_PEAK, height * GALLERY_PEAK_TOP)
            lineTo(width * GALLERY_VALLEY, height * GALLERY_VALLEY_TOP)
            lineTo(width * GALLERY_RIDGE, height * GALLERY_RIDGE_TOP)
            lineTo(width * GALLERY_END, height * GALLERY_BASE)
        }
        drawPath(path, color, style = stroke())
    }
}

/** Молния вспышки. Замкнутая ломаная: разомкнутая читалась бы как галочка. */
@Composable
fun SlimlyFlash(modifier: Modifier = Modifier, size: Dp = 13.dp, color: Color = SlimlyColor.Text) {
    Canvas(modifier.size(width = size, height = size * FLASH_RATIO)) {
        val width = this.size.width
        val height = this.size.height
        val path = Path().apply {
            moveTo(width * FLASH_TOP, 0f)
            lineTo(0f, height * FLASH_WAIST)
            lineTo(width * FLASH_INNER_LEFT, height * FLASH_WAIST)
            lineTo(width * FLASH_BOTTOM, height)
            lineTo(width, height * FLASH_SHOULDER)
            lineTo(width * FLASH_INNER_RIGHT, height * FLASH_SHOULDER)
            close()
        }
        drawPath(path, color, style = stroke())
    }
}

/** Карандаш правки имени. */
@Composable
fun SlimlyPencil(modifier: Modifier = Modifier, size: Dp = 17.dp, color: Color = SlimlyColor.Neutral500) {
    Canvas(modifier.size(size)) {
        val side = this.size.width
        val path = Path().apply {
            // Корпус — параллелограмм под 45°, остриё — точка на его оси.
            moveTo(0f, side)
            lineTo(side * PENCIL_NEAR, side * PENCIL_SHAFT)
            lineTo(side * PENCIL_SHAFT, side * PENCIL_NEAR)
            lineTo(side * PENCIL_FAR, side * PENCIL_COLLAR)
            lineTo(side * PENCIL_COLLAR, side * PENCIL_FAR)
            close()

            // Обойма поперёк корпуса: без неё остриё сливается с корпусом
            // в одну стрелку.
            moveTo(side * PENCIL_NEAR, side * PENCIL_SHAFT)
            lineTo(side * PENCIL_COLLAR, side * PENCIL_FAR)
        }
        drawPath(path, color, style = stroke())
    }
}

/**
 * Три узла и две связи между ними — знак «поделиться» в андроидной повадке.
 *
 * Не иосная стрелка из коробки: жест системный, и человек ищет глазами
 * ровно тот значок, который стоит у него во всех остальных приложениях.
 */
@Composable
fun SlimlyShare(modifier: Modifier = Modifier, size: Dp = 17.dp, color: Color = SlimlyColor.Accent300) {
    Canvas(modifier.size(size)) {
        val side = this.size.width
        val radius = side * SHARE_NODE

        drawCircle(color, radius, Offset(side * SHARE_FAR, side * SHARE_TOP), style = stroke())
        drawCircle(color, radius, Offset(side * SHARE_NEAR, side * SHARE_MIDDLE), style = stroke())
        drawCircle(color, radius, Offset(side * SHARE_FAR, side * (1f - SHARE_TOP)), style = stroke())

        // Связь начинается от края узла, а не от его центра: иначе линия
        // проходит сквозь кружок и знак читается как перечёркнутый.
        val path = Path().apply {
            moveTo(side * SHARE_LINK_NEAR, side * SHARE_LINK_INNER)
            lineTo(side * SHARE_LINK_FAR, side * SHARE_LINK_OUTER)
            moveTo(side * SHARE_LINK_NEAR, side * (1f - SHARE_LINK_INNER))
            lineTo(side * SHARE_LINK_FAR, side * (1f - SHARE_LINK_OUTER))
        }
        drawPath(path, color, style = stroke())
    }
}

private fun stroke() = Stroke(width = STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Толщина линии одна на все иконки: разнобой виден даже на десяти пунктах. */
private const val STROKE = 3.6f

private const val CHEVRON_RATIO = 1.9f
private const val CHECK_RATIO = 0.78f
private const val ARROW_RATIO = 0.62f
private const val ARROW_HEAD = 0.5f
private const val CHECK_TURN = 0.5f
private const val CHECK_CORNER = 0.36f

private const val CAMERA_RATIO = 0.85f
private const val CAMERA_CORNER = 0.14f
private const val CAMERA_BODY_TOP = 0.16f
private const val CAMERA_BUMP_INNER_LEFT = 0.24f
private const val CAMERA_BUMP_OUTER_LEFT = 0.31f
private const val CAMERA_BUMP_OUTER_RIGHT = 0.69f
private const val CAMERA_BUMP_INNER_RIGHT = 0.76f
private const val CAMERA_LENS_CENTER = 0.6f
private const val CAMERA_LENS_RADIUS = 0.2f

private const val GALLERY_RATIO = 0.86f
private const val GALLERY_CORNER = 0.14f
private const val GALLERY_START = 0.15f
private const val GALLERY_BASE = 0.8f
private const val GALLERY_PEAK = 0.4f
private const val GALLERY_PEAK_TOP = 0.49f
private const val GALLERY_VALLEY = 0.58f
private const val GALLERY_VALLEY_TOP = 0.71f
private const val GALLERY_RIDGE = 0.71f
private const val GALLERY_RIDGE_TOP = 0.55f
private const val GALLERY_END = 0.86f

private const val PENCIL_NEAR = 0.1f
private const val PENCIL_COLLAR = 0.3f
private const val PENCIL_SHAFT = 0.7f
private const val PENCIL_FAR = 0.9f

private const val FLASH_RATIO = 1.55f
private const val FLASH_TOP = 0.65f
private const val FLASH_WAIST = 0.57f
private const val FLASH_INNER_LEFT = 0.41f
private const val FLASH_BOTTOM = 0.34f
private const val FLASH_SHOULDER = 0.42f
private const val FLASH_INNER_RIGHT = 0.57f

private const val SHARE_NODE = 0.13f
private const val SHARE_NEAR = 0.22f
private const val SHARE_FAR = 0.78f
private const val SHARE_TOP = 0.2f
private const val SHARE_MIDDLE = 0.5f
private const val SHARE_LINK_NEAR = 0.335f
private const val SHARE_LINK_FAR = 0.665f
private const val SHARE_LINK_INNER = 0.439f
private const val SHARE_LINK_OUTER = 0.261f
