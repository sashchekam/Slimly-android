package com.slimly.feature.scan

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyStripeStyle
import com.slimly.designsystem.SlimlyStripes
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap

/**
 * Шаг «границы страниц»: страница крупно с рамкой, лента остальных внизу.
 *
 * Один экран на все страницы, а не по экрану на каждую: границы правят
 * подряд, и уходить-возвращаться ради каждой значило бы делать из одного
 * дела пять.
 */
@Composable
fun ScanPagesScreen(viewModel: ScanPagesViewModel, modifier: Modifier = Modifier) {
    LaunchedEffect(viewModel) { viewModel.load() }

    SlimlyScreen(
        modifier = modifier,
        onBack = viewModel::goBack,
        bottomAction = { SlimlyButton(title = viewModel.ctaTitle, onClick = viewModel::proceed) },
    ) {
        // Экран не скроллится намеренно: углы рамки тянут пальцем, и
        // вертикальная прокрутка перехватывала бы ровно то движение, которым
        // её и правят. Кадр забирает всё оставшееся место — на маленьком
        // экране он просто меньше.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SlimlySpacing.ScreenHorizontal),
            verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X4),
        ) {
            Header(viewModel)
            Editor(viewModel, Modifier.weight(1f))
            Hint(viewModel)
            Actions(viewModel)

            if (viewModel.hasMultiplePages) Strip(viewModel)

            // Место под кнопку внизу: она приклеена к экрану поверх столбца,
            // и без запаса лента страниц ушла бы под неё.
            Box(Modifier.height(BOTTOM_RESERVE))
        }
    }
}

@Composable
private fun Header(viewModel: ScanPagesViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2)) {
        Text(text = viewModel.title, style = SlimlyText.Title, color = SlimlyColor.Text)
        Text(text = viewModel.subtitle, style = SlimlyText.BodySmall, color = SlimlyColor.Neutral400)
    }
}

@Composable
private fun Editor(viewModel: ScanPagesViewModel, modifier: Modifier = Modifier) {
    val page = viewModel.page

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        PageBoundsEditor(
            // Кадр меняется вместе со страницей, и без ключа Compose переносит
            // на новую страницу состояние перетаскивания со старой.
            image = remember(page.id, page.frame) { page.frame?.asImageBitmap() },
            fallbackAspectRatio = aspectRatio(viewModel),
            quad = page.quad,
            onMove = viewModel::move,
            // Ручки углов наполовину выходят за кадр — без запаса они
            // обрезались бы о края экрана ровно там, где за них и тянут.
            modifier = Modifier.padding(horizontal = HANDLE_ROOM),
        )
    }
}

private fun aspectRatio(viewModel: ScanPagesViewModel): Float {
    val item = viewModel.page.item
    if (item.pixelWidth <= 0 || item.pixelHeight <= 0) return DEFAULT_ASPECT_RATIO
    return item.pixelWidth.toFloat() / item.pixelHeight
}

@Composable
private fun Hint(viewModel: ScanPagesViewModel) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(DOT)
                .background(
                    if (viewModel.page.isReady) SlimlyColor.Accent else SlimlyColor.Neutral600,
                    CircleShape,
                ),
        )
        Text(text = viewModel.hint, style = SlimlyText.Caption, color = SlimlyColor.Neutral400)
    }
}

/**
 * Обе кнопки — на случай, когда что-то пошло не так: вернуть найденное после
 * случайной правки и распрямить рамку по кадру, когда обрезать нечего.
 * В обычном ходе дел они не нужны, поэтому и не показываются — но место
 * под них держим всегда: кадр забирает остаток экрана, и появись строка
 * внезапно, страница уехала бы прямо под пальцем.
 */
@Composable
private fun Actions(viewModel: ScanPagesViewModel) {
    Row(
        modifier = Modifier.height(ACTIONS_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = viewModel.canResetToDetected) {
            CompactAction(titleRes = R.string.pages_reset, onClick = viewModel::resetToDetected)
        }
        AnimatedVisibility(visible = viewModel.canUseWholeFrame) {
            CompactAction(titleRes = R.string.pages_no_crop, onClick = viewModel::useWholeFrame)
        }
    }
}

@Composable
private fun CompactAction(@StringRes titleRes: Int, onClick: () -> Unit) {
    Text(
        text = stringResource(titleRes),
        style = SlimlyText.ButtonSmall,
        color = SlimlyColor.Accent300,
        modifier = Modifier
            .clip(RoundedCornerShape(SlimlyRadius.Pill))
            .background(SlimlyColor.Surface)
            .slimlyTap(onClick = onClick)
            .padding(horizontal = SlimlySpacing.X4, vertical = SlimlySpacing.X2),
    )
}

/**
 * Лента страниц: миниатюра с той же рамкой, что стоит на большом кадре, —
 * по ней видно, где границы уже поправлены, а где ещё нет.
 */
@Composable
private fun Strip(viewModel: ScanPagesViewModel) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X3),
        contentPadding = PaddingValues(vertical = SlimlySpacing.X1),
    ) {
        itemsIndexed(viewModel.pages, key = { _, page -> page.id }) { position, page ->
            PageThumbnail(
                page = page,
                number = position + 1,
                isCurrent = viewModel.isCurrent(page),
                onClick = { viewModel.select(position) },
            )
        }
    }
}

/** Страница в ленте: кадр, номер и рамка поверх. */
@Composable
private fun PageThumbnail(page: ScanPagesViewModel.Page, number: Int, isCurrent: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(SlimlyRadius.Tile)

    Box(
        modifier = Modifier
            .size(THUMBNAIL)
            .clip(shape)
            .border(
                width = if (isCurrent) CURRENT_EDGE else EDGE,
                color = if (isCurrent) SlimlyColor.Accent else SlimlyColor.Text.copy(alpha = EDGE_ALPHA),
                shape = shape,
            )
            .slimlyTap(onClick = onClick),
    ) {
        SlimlyStripes(Modifier.fillMaxSize(), SlimlyStripeStyle.THUMBNAIL)

        val frame = remember(page.id, page.frame) { page.frame?.asImageBitmap() }
        if (frame != null) {
            Image(
                bitmap = frame,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Та же рамка, что и на большом кадре: миниатюра обрезана до квадрата,
        // поэтому линия здесь не обещание, а напоминание, — но по ней сразу
        // видно, какая страница уже размечена.
        Canvas(Modifier.fillMaxSize()) {
            val points = page.quad.corners.map { point(it, size) }
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            drawPath(
                path,
                SlimlyColor.Accent200.copy(alpha = OUTLINE_ALPHA),
                style = Stroke(width = EDGE.toPx(), cap = StrokeCap.Round),
            )
        }

        Text(
            text = number.toString(),
            style = SlimlyText.Nano,
            color = SlimlyColor.Neutral200,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(NUMBER_INSET)
                .background(SlimlyColor.TileCaption, RoundedCornerShape(SlimlyRadius.Sm))
                .padding(horizontal = SlimlySpacing.X1),
        )

        // Точка вместо подписи: место есть только на неё, а знать нужно
        // одно — эту страницу человек уже размечал сам.
        if (page.isManual) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(MARK_INSET)
                    .size(MARK)
                    .background(SlimlyColor.Accent, CircleShape),
            )
        }
    }
}

private const val DEFAULT_ASPECT_RATIO = 3f / 4f

private val ACTIONS_HEIGHT = 34.dp
private val BOTTOM_RESERVE = 72.dp
private val HANDLE_ROOM = 12.dp
private val DOT = 6.dp

private val THUMBNAIL = 58.dp
private val EDGE = 1.dp
private val CURRENT_EDGE = 2.dp
private val NUMBER_INSET = 4.dp
private val MARK_INSET = 5.dp
private val MARK = 7.dp
private const val EDGE_ALPHA = 0.12f
private const val OUTLINE_ALPHA = 0.9f
