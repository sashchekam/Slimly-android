package com.slimly.mediaflow.picker

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slimly.core.model.MediaAlbum
import com.slimly.core.model.MediaItem
import com.slimly.designsystem.SlimlyButton
import com.slimly.designsystem.SlimlyCheck
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyProgressBar
import com.slimly.designsystem.SlimlyRadius
import com.slimly.designsystem.SlimlyScreen
import com.slimly.designsystem.SlimlySpacing
import com.slimly.designsystem.SlimlyText
import com.slimly.designsystem.slimlyTap
import com.slimly.mediaflow.MediaThumbnail
import com.slimly.mediaflow.R

/**
 * Выбор файлов — первый шаг любого инструмента.
 *
 * Своя витрина, а не системный пикер: он приходит со своим интерфейсом,
 * своими правилами и без веса файлов, а весь смысл шага — видеть, сколько
 * весит то, что берёшь.
 */
@Composable
fun MediaPickerScreen(viewModel: MediaPickerViewModel, modifier: Modifier = Modifier) {
    val permissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.accessAnswered() }

    LaunchedEffect(viewModel) { viewModel.load() }

    // Запрос показываем сами, без кнопки-посредника: человек пришёл выбирать
    // файлы, и лишний экран «сейчас мы спросим доступ» ему ничего не сообщит.
    LaunchedEffect(viewModel.access, viewModel.isLoading, viewModel.hasRequestedAccess) {
        if (!viewModel.isLoading && !viewModel.hasRequestedAccess && !viewModel.access.allowsBrowsing) {
            viewModel.accessRequested()
            permissions.launch(viewModel.permissions.toTypedArray())
        }
    }

    SlimlyScreen(
        modifier = modifier,
        onBack = viewModel::goBack,
        bottomAction = if (viewModel.hasSelection) {
            { PickerAction(viewModel) }
        } else {
            null
        },
    ) {
        Column(Modifier.padding(horizontal = SlimlySpacing.ScreenHorizontal)) {
            Text(text = viewModel.config.title, style = SlimlyText.LargeTitle, color = SlimlyColor.Text)
            Text(
                text = viewModel.config.hint,
                style = SlimlyText.Body,
                color = SlimlyColor.Neutral500,
                modifier = Modifier.padding(top = SlimlySpacing.X2),
            )
        }

        when {
            viewModel.isDenied -> AccessDenied()
            viewModel.isEmpty -> EmptyLibrary(isLimited = viewModel.isLimited)
            else -> MediaGrid(viewModel)
        }
    }
}

@Composable
private fun PickerAction(viewModel: MediaPickerViewModel) {
    Column {
        SlimlyProgressBar(
            value = viewModel.limitFraction,
            modifier = Modifier.padding(bottom = SlimlySpacing.X3),
            height = 3.dp,
        )
        SlimlyButton(
            title = if (viewModel.config.showsSize) {
                stringResource(R.string.picker_action_with_size, viewModel.selectionText, viewModel.selectionSizeText)
            } else {
                stringResource(R.string.picker_action, viewModel.selectionText)
            },
            onClick = viewModel::proceed,
        )
    }
}

@Composable
private fun MediaGrid(viewModel: MediaPickerViewModel) {
    val grid = rememberLazyGridState()
    val albums = viewModel.albums

    if (albums.size > 1) {
        AlbumRow(albums = albums, selectedId = viewModel.albumId, onSelect = viewModel::select)
    }

    VisibleDates(viewModel = viewModel, grid = grid)

    // Продолжение просим за пару рядов до конца: пауза посреди прокрутки
    // читалась бы как конец списка.
    val needsMore by remember(viewModel) {
        derivedStateOf {
            val last = grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= viewModel.items.size - PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(needsMore) {
        if (needsMore) viewModel.loadMoreIfNeeded()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SlimlySpacing.ScreenHorizontal,
            end = SlimlySpacing.ScreenHorizontal,
            top = SlimlySpacing.X4,
            bottom = BOTTOM_INSET,
        ),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
    ) {
        items(items = viewModel.items, key = { it.id }) { item ->
            MediaTile(
                item = item,
                viewModel = viewModel,
                modifier = Modifier.aspectRatio(1f),
            )
        }
    }
}

/**
 * Отрезок дат, попавших в кадр, — как в системной галерее.
 *
 * Считается по видимым плиткам сетки: медиатека идёт от свежих файлов
 * к старым, и надпись отвечает на «докуда долистали». Отдельная функция,
 * а не строка в шапке: пересчёт идёт на каждом кадре прокрутки, и трогать
 * он должен одну надпись, а не сетку под ней.
 */
@Composable
private fun VisibleDates(viewModel: MediaPickerViewModel, grid: LazyGridState) {
    val dates by remember(viewModel) {
        derivedStateOf { viewModel.visibleDates(grid.layoutInfo.visibleItemsInfo.map { it.index }) }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SlimlySpacing.ScreenHorizontal, vertical = SlimlySpacing.X2)
            .height(DATES_HEIGHT),
        contentAlignment = Alignment.CenterEnd,
    ) {
        // Надпись гаснет, а не пропадает: при перелистывании альбома даты
        // на миг исчезают, и мигание строки читалось бы как сбой.
        AnimatedVisibility(visible = dates != null, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = dates.orEmpty(),
                style = SlimlyText.CaptionSmall,
                color = SlimlyColor.Neutral500,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MediaTile(item: MediaItem, viewModel: MediaPickerViewModel, modifier: Modifier = Modifier) {
    val isSelected = viewModel.isSelected(item)
    val isBlocked = viewModel.isBlocked(item)
    val shape = RoundedCornerShape(SlimlyRadius.Tile)

    Box(
        modifier = modifier
            .clip(shape)
            .alpha(if (isBlocked) BLOCKED_ALPHA else 1f)
            .slimlyTap(enabled = !isBlocked) { viewModel.toggle(item) },
    ) {
        MediaThumbnail(
            item = item,
            thumbnails = viewModel.thumbnails,
            size = THUMBNAIL_SIZE,
            modifier = Modifier.fillMaxSize(),
        )

        if (isSelected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(width = 2.dp, color = SlimlyColor.Accent, shape = shape),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(SlimlySpacing.X2)
                    .size(CHECK_SIZE)
                    .background(SlimlyColor.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SlimlyCheck()
            }
        }
    }
}

@Composable
private fun AlbumRow(albums: List<MediaAlbum>, selectedId: String, onSelect: (MediaAlbum) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = SlimlySpacing.X4),
        contentPadding = PaddingValues(horizontal = SlimlySpacing.ScreenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(SlimlySpacing.X2),
    ) {
        items(items = albums, key = { it.id }) { album ->
            val isSelected = album.id == selectedId
            Text(
                text = album.title,
                style = SlimlyText.Pill,
                color = if (isSelected) SlimlyColor.Accent200 else SlimlyColor.Neutral400,
                modifier = Modifier
                    .clip(RoundedCornerShape(SlimlyRadius.Pill))
                    .background(if (isSelected) SlimlyColor.Accent.copy(alpha = 0.14f) else SlimlyColor.Surface)
                    .slimlyTap { onSelect(album) }
                    .padding(horizontal = SlimlySpacing.X4, vertical = SlimlySpacing.X2),
            )
        }
    }
}

@Composable
private fun AccessDenied() {
    val context = LocalContext.current

    Message(
        title = stringResource(R.string.picker_denied_title),
        hint = stringResource(R.string.picker_denied_hint),
        action = stringResource(R.string.picker_denied_action),
        onAction = {
            // Второй раз система запрос не покажет: разрешение возвращают
            // только в настройках приложения.
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
    )
}

@Composable
private fun EmptyLibrary(isLimited: Boolean) {
    Message(
        title = stringResource(R.string.picker_empty_title),
        hint = stringResource(
            if (isLimited) R.string.picker_empty_limited else R.string.picker_empty_hint,
        ),
    )
}

@Composable
private fun Message(title: String, hint: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SlimlySpacing.ScreenHorizontal),
        verticalArrangement = Arrangement.spacedBy(SlimlySpacing.X3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = SlimlyText.SectionTitle, color = SlimlyColor.Text)
        Text(text = hint, style = SlimlyText.Body, color = SlimlyColor.Neutral500)

        if (action != null && onAction != null) {
            Row(Modifier.padding(top = SlimlySpacing.X3)) {
                SlimlyButton(title = action, onClick = onAction)
            }
        }
    }
}

private const val COLUMNS = 3
private const val THUMBNAIL_SIZE = 320
private val DATES_HEIGHT = 16.dp
private const val PREFETCH_DISTANCE = 24
private const val BLOCKED_ALPHA = 0.35f
private val CHECK_SIZE = 20.dp
private val BOTTOM_INSET = 120.dp
