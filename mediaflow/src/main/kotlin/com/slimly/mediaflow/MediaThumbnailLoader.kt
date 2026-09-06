package com.slimly.mediaflow

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.slimly.core.model.MediaItem
import com.slimly.core.service.MediaLibrary
import com.slimly.designsystem.SlimlyColor

/**
 * Загрузчик миниатюр — всё, что экранам нужно от медиатеки для показа кадра.
 *
 * Отдельным интерфейсом, а не всей библиотекой: плитке незачем уметь
 * сохранять файлы и удалять оригиналы.
 */
fun interface MediaThumbnailLoader {
    suspend fun thumbnail(item: MediaItem, size: Int): Bitmap?
}

val MediaLibrary.thumbnailLoader: MediaThumbnailLoader
    get() = MediaThumbnailLoader { item, size -> thumbnail(item, size) }

/**
 * Кадр файла. Пока кадра нет, на месте плитки лежит поверхность, а не пустота:
 * при прокрутке дыры в сетке читаются как поломка, а ровный прямоугольник —
 * как «сейчас будет».
 */
@Composable
fun MediaThumbnail(item: MediaItem, thumbnails: MediaThumbnailLoader, size: Int, modifier: Modifier = Modifier) {
    var frame by remember(item.id, size) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.id, size) {
        frame = thumbnails.thumbnail(item, size)?.asImageBitmap()
    }

    Box(modifier.background(SlimlyColor.SurfaceSunken)) {
        val bitmap = frame
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
