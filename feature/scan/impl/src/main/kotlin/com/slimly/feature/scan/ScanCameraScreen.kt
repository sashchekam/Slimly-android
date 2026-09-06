package com.slimly.feature.scan

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.slimly.core.model.DocumentQuad
import com.slimly.designsystem.SlimlyCamera
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyFlash
import com.slimly.designsystem.SlimlyHaptic
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.SlimlyToastHost
import com.slimly.designsystem.rememberSlimlyHaptics
import com.slimly.designsystem.slimlyTap
import kotlin.math.hypot

/**
 * Съёмка страниц документа.
 *
 * Единственный экран приложения без общей обвязки: под кадром камеры
 * градиентный фон и поля Nocturne только мешали бы — здесь весь контраст
 * держится на самом кадре, а интерфейс живёт поверх него.
 */
@Composable
fun ScanCameraScreen(viewModel: ScanCameraViewModel, modifier: Modifier = Modifier) {
    val permission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.accessAnswered() }

    LaunchedEffect(viewModel) { viewModel.observe() }

    // Камера занимает устройство, поэтому включается вместе с экраном
    // и гаснет вместе с ним — даже если сам экран остался в памяти.
    DisposableEffect(viewModel) {
        viewModel.start()
        onDispose { viewModel.stop() }
    }

    LaunchedEffect(viewModel.access, viewModel.hasRequestedAccess) {
        if (!viewModel.hasRequestedAccess && !viewModel.isDenied) {
            viewModel.accessRequested()
            permission.launch(viewModel.permission)
        }
    }

    // Системный «назад» здесь равен «Отмене»: закрыть камеру, не выбросив
    // снятое, значит оставить страницы лежать в кэше до перезапуска.
    BackHandler { viewModel.cancel() }

    // Страницу снимает автоспуск, а не палец: без отдачи момент съёмки
    // видно только по стопке в углу — а на неё в этот момент не смотрят.
    val haptics = rememberSlimlyHaptics()
    LaunchedEffect(viewModel.pageCount) {
        if (viewModel.pageCount > 0) haptics(SlimlyHaptic.IMPACT)
    }

    Box(modifier.fillMaxSize().background(VIEWFINDER)) {
        if (viewModel.isDenied) {
            DeniedNotice(Modifier.align(Alignment.Center))
        } else {
            Viewfinder(viewModel)
        }

        Controls(viewModel)
    }
}

// MARK: Кадр

@Composable
private fun Viewfinder(viewModel: ScanCameraViewModel) {
    Box(Modifier.fillMaxSize()) {
        // Превью строит сама сессия: системный слой камеры за границу
        // `:mediakit` не выходит, и экран получает его готовым `View`.
        AndroidView(
            factory = { viewModel.camera.previewView(it) },
            modifier = Modifier.fillMaxSize(),
        )

        val quad = viewModel.state.quad
        if (quad != null) {
            DocumentFrame(quad = quad, frameAspectRatio = viewModel.state.aspectRatio)
        }
    }
}

/**
 * Найденные границы листа поверх кадра.
 *
 * Рисуем сам четырёхугольник, а не прямоугольник вокруг него: обещание
 * рамки должно совпадать с тем, что потом вырежется, иначе кадр выравнивают
 * по одной границе, а получают другую.
 */
@Composable
private fun DocumentFrame(quad: DocumentQuad, frameAspectRatio: Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val points = fitted(quad, size, frameAspectRatio)
        val outline = closedPath(points)

        drawPath(outline, SlimlyColor.Accent.copy(alpha = FRAME_FILL))
        drawPath(outline, SlimlyColor.Accent, style = Stroke(width = FRAME_EDGE.toPx()))
        drawPath(
            corners(points, FRAME_CORNER.toPx()),
            SlimlyColor.Accent200,
            style = Stroke(width = FRAME_CORNER_EDGE.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Доли кадра — в точки экрана.
 *
 * Превью вписано в экран целиком, а не заполняет его: сессия ставит
 * `FIT_CENTER`, поэтому кадр лежит с полями по короткой стороне, и рамка
 * считается по тому же правилу.
 */
private fun fitted(quad: DocumentQuad, size: Size, aspectRatio: Float): List<Offset> {
    val width: Float
    val height: Float
    if (size.width / size.height > aspectRatio) {
        height = size.height
        width = height * aspectRatio
    } else {
        width = size.width
        height = width / aspectRatio
    }

    val left = (size.width - width) / 2
    val top = (size.height - height) / 2

    return quad.corners.map { Offset(left + it.x * width, top + it.y * height) }
}

private fun closedPath(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

/**
 * Короткие усы у каждого угла — по ним видно, куда именно смотрит рамка,
 * даже когда сама линия теряется на светлой странице.
 */
private fun corners(points: List<Offset>, length: Float): Path = Path().apply {
    points.forEachIndexed { index, corner ->
        listOf(points.previous(index), points.next(index)).forEach { neighbour ->
            moveTo(corner.x, corner.y)
            val target = corner.approaching(neighbour, length)
            lineTo(target.x, target.y)
        }
    }
}

/** Точка на отрезке к соседу: длиной в [distance], но не длиннее доли стороны. */
private fun Offset.approaching(other: Offset, distance: Float): Offset {
    val side = hypot(other.x - x, other.y - y)
    if (side <= 0f) return this
    val fraction = minOf(distance / side, CORNER_SHARE)
    return Offset(x + (other.x - x) * fraction, y + (other.y - y) * fraction)
}

// MARK: Отказ

/**
 * Без доступа камера не покажет ничего, поэтому вместо чёрного
 * прямоугольника — прямая дорога в настройки.
 */
@Composable
private fun DeniedNotice(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(horizontal = DENIED_HORIZONTAL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
    ) {
        SlimlyCamera(size = DENIED_ICON, color = SlimlyColor.Neutral500)

        Text(
            text = stringResource(R.string.camera_denied_title),
            style = SlimlyText.SectionTitle,
            color = SlimlyColor.Text,
        )
        Text(
            text = stringResource(R.string.camera_denied_note),
            style = SlimlyText.BodySmall,
            color = SlimlyColor.Neutral400,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.camera_open_settings),
            style = SlimlyText.ButtonSmall,
            color = SlimlyColor.Accent300,
            modifier = Modifier
                .slimlyTap {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
                .padding(SlimlySpacing.X2),
        )
    }
}

// MARK: Управление

@Composable
private fun Controls(viewModel: ScanCameraViewModel) {
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        TopBar(viewModel, Modifier.align(Alignment.TopCenter))

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Hint(viewModel.hintText)
            ShutterBar(viewModel)
        }

        // Тост живёт над подсказкой, а не у нижнего края: низ экрана занят
        // затвором, и общий хост накрыл бы его.
        Box(Modifier.fillMaxSize().padding(bottom = TOAST_LIFT)) {
            SlimlyToastHost(viewModel.toast)
        }
    }
}

@Composable
private fun TopBar(viewModel: ScanCameraViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SlimlySpacing.X6, vertical = SlimlySpacing.X2),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.camera_cancel),
            style = SlimlyText.Body,
            color = SlimlyColor.Text,
            modifier = Modifier.slimlyTap(onClick = viewModel::cancel).padding(SlimlySpacing.X2),
        )

        Box(Modifier.weight(1f))

        val flashLabel = stringResource(R.string.camera_flash, viewModel.flashLabel)
        ModeChip(
            isOn = viewModel.isFlashOn,
            onClick = viewModel::toggleFlash,
            modifier = Modifier.semantics { contentDescription = flashLabel },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlimlyFlash(color = if (viewModel.isFlashOn) SlimlyColor.Accent100 else SlimlyColor.Text)
                ChipLabel(viewModel.flashLabel, viewModel.isFlashOn)
            }
        }

        ModeChip(isOn = viewModel.isAutomatic, onClick = viewModel::toggleAutomatic) {
            ChipLabel(viewModel.automaticLabel, viewModel.isAutomatic)
        }
    }
}

/**
 * Чип режима: включённый берёт акцентный тинт, выключенный — нейтральную
 * подложку. Рамки нет: поверх живого кадра она читается хуже, чем заливка.
 */
@Composable
private fun ModeChip(
    isOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SlimlyRadius.Pill))
            .background(
                if (isOn) {
                    SlimlyColor.Accent.copy(alpha = CHIP_ACCENT_FILL)
                } else {
                    SlimlyColor.Text.copy(alpha = CHIP_PLAIN_FILL)
                },
            )
            .slimlyTap(onClick = onClick)
            .padding(horizontal = SlimlySpacing.X4, vertical = SlimlySpacing.X2),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ChipLabel(text: String, isOn: Boolean) {
    Text(
        text = text,
        style = SlimlyText.Pill,
        color = if (isOn) SlimlyColor.Accent100 else SlimlyColor.Text,
    )
}

@Composable
private fun Hint(text: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = SlimlySpacing.ScreenHorizontal, vertical = SlimlySpacing.X4)
            .clip(RoundedCornerShape(SlimlyRadius.Pill))
            .background(VIEWFINDER.copy(alpha = HINT_FILL))
            .border(
                width = HINT_EDGE,
                color = SlimlyColor.Accent.copy(alpha = HINT_EDGE_ALPHA),
                shape = RoundedCornerShape(SlimlyRadius.Pill),
            )
            .padding(horizontal = HINT_HORIZONTAL, vertical = SlimlySpacing.X3),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(HINT_DOT).background(SlimlyColor.Accent, CircleShape))
        Text(text = text, style = SlimlyText.BodySmall, color = SlimlyColor.Text)
    }
}

@Composable
private fun ShutterBar(viewModel: ScanCameraViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(VIEWFINDER.copy(alpha = 0f), VIEWFINDER.copy(alpha = BAR_FILL))),
            )
            .padding(horizontal = BAR_HORIZONTAL, vertical = SlimlySpacing.X6),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Counter(viewModel)
        }

        Shutter(
            enabled = viewModel.canShoot,
            isAtLimit = viewModel.state.isAtLimit,
            label = stringResource(R.string.camera_shutter),
            onClick = viewModel::shoot,
        )

        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = viewModel.doneTitle,
                style = SlimlyText.ButtonSmall,
                color = if (viewModel.hasPages) SlimlyColor.Accent200 else SlimlyColor.Neutral600,
                modifier = Modifier
                    .slimlyTap(enabled = viewModel.hasPages, onClick = viewModel::finish)
                    .padding(SlimlySpacing.X2),
            )
        }
    }
}

@Composable
private fun Counter(viewModel: ScanCameraViewModel) {
    if (!viewModel.hasPages) {
        Text(
            text = stringResource(R.string.camera_first_page),
            style = SlimlyText.Caption,
            color = SlimlyColor.Neutral500,
        )
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PageStack(viewModel.pageCount)
        Text(text = viewModel.pagesText, style = SlimlyText.Caption, color = SlimlyColor.Neutral400)
    }
}

/** Стопка снятых страниц: верхний лист с номером и краешек нижнего. */
@Composable
private fun PageStack(count: Int) {
    Box(Modifier.size(STACK), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = STACK_SHIFT, top = STACK_SHIFT)
                .background(SlimlyColor.Neutral500, RoundedCornerShape(SlimlyRadius.Md)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = STACK_SHIFT, bottom = STACK_SHIFT)
                .background(
                    Brush.linearGradient(listOf(SlimlyColor.PaperLight, SlimlyColor.PaperDark)),
                    RoundedCornerShape(SlimlyRadius.Md),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = count.toString(), style = SlimlyText.ButtonSmall, color = SlimlyColor.OnAccent)
        }
    }
}

/** Затвор откликается только приглушением: подсветить его нечем — он белый. */
@Composable
private fun Shutter(enabled: Boolean, isAtLimit: Boolean, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(SHUTTER)
            .alpha(if (isAtLimit) SHUTTER_SPENT else 1f)
            .semantics { contentDescription = label }
            .slimlyTap(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(SlimlyColor.Text, radius = radius, style = Stroke(width = SHUTTER_RING.toPx()))
            drawCircle(SlimlyColor.Text, radius = radius - SHUTTER_GAP.toPx())
        }
    }
}

/**
 * Фон видоискателя темнее основного: под живым кадром любой оттенок фона
 * читается как засветка по краям.
 */
private val VIEWFINDER = Color(0xFF0A0B10)

private val FRAME_EDGE = 2.dp
private val FRAME_CORNER = 22.dp
private val FRAME_CORNER_EDGE = 3.dp
private const val FRAME_FILL = 0.12f

/** Уголок не длиннее четверти стороны, иначе на мелком листе они смыкаются. */
private const val CORNER_SHARE = 0.25f

private val DENIED_HORIZONTAL = 44.dp
private val DENIED_ICON = 34.dp

private const val CHIP_ACCENT_FILL = 0.26f
private const val CHIP_PLAIN_FILL = 0.12f

private val HINT_DOT = 6.dp
private val HINT_EDGE = 1.dp
private val HINT_HORIZONTAL = 13.dp
private const val HINT_FILL = 0.62f
private const val HINT_EDGE_ALPHA = 0.35f

private val TOAST_LIFT = 96.dp
private val BAR_HORIZONTAL = 22.dp
private const val BAR_FILL = 0.9f

private val STACK = 44.dp
private val STACK_SHIFT = 4.dp

private val SHUTTER = 70.dp
private val SHUTTER_RING = 3.dp
private val SHUTTER_GAP = 7.dp
private const val SHUTTER_SPENT = 0.4f
