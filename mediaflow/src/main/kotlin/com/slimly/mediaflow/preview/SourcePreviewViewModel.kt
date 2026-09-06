package com.slimly.mediaflow.preview

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.mediaflow.MediaThumbnailLoader
import com.slimly.mediaflow.R
import com.slimly.navigation.Router

/**
 * Просмотр исходного файла — того, что ещё только предстоит обработать.
 *
 * Нужен до обработки, а не после: на шаге выбора формата от файла остаются
 * имя и миниатюра в 33 точки, и по ним не всегда видно, тот ли это кадр.
 * От [ResultPreviewViewModel] отличается тем, что сравнивать здесь не с чем:
 * результата ещё нет, есть только оригинал.
 */
class SourcePreviewViewModel(
    val item: MediaItem,
    private val thumbnails: MediaThumbnailLoader,
    private val formatters: SlimlyFormatters,
    private val resources: Resources,
    private val router: Router,
    /** Показывать ли вес: у скана он ни на что не влияет, как и в списке. */
    private val showsSize: Boolean = true,
) {
    var frame: Bitmap? by mutableStateOf(null)
        private set

    /**
     * Кадр просим крупный: миниатюра списка на весь экран рассыпалась бы
     * в пиксели, а оригинал целиком ради просмотра тянуть незачем.
     */
    suspend fun load() {
        if (frame == null) frame = thumbnails.thumbnail(item, PREVIEW_PIXEL)
    }

    val title: String
        get() = item.fileName

    /**
     * «Фото · 4032 × 3024 · 3,1 МБ» — пустые части выпадают, если система
     * не сообщила размер кадра или инструменту не нужен вес.
     */
    val subtitle: String
        get() = listOf(
            formatters.items.kindLabel(item),
            item.pixelSizeText,
            formatters.items.meta(item, showsSize = showsSize),
        ).filter { it.isNotEmpty() }.joinToString(separator = " · ")

    /**
     * Пропорции окна просмотра: у кадра известного размера — его собственные,
     * иначе лист для снимка и кадр для ролика.
     */
    val previewAspectRatio: Float
        get() {
            if (item.pixelWidth <= 0 || item.pixelHeight <= 0) {
                return if (item.kind == MediaKind.VIDEO) VIDEO_ASPECT else PHOTO_ASPECT
            }
            return item.pixelWidth.toFloat() / item.pixelHeight
        }

    /**
     * Пока кадр не приехал, подписываем, что именно грузится: пустая
     * штриховка без подписи читалась бы как ошибка.
     */
    val placeholderLabel: String
        get() = if (item.kind == MediaKind.VIDEO) {
            formatters.items.kindLabel(item)
        } else {
            resources.getString(R.string.badge_original)
        }

    /**
     * Плеера у просмотра нет, и обещать его подписью нельзя: ролик здесь —
     * это кадр, а смотрят его системным приложением.
     */
    val note: String
        get() = resources.getString(R.string.preview_source_note)

    fun goBack() {
        router.pop()
    }

    private companion object {
        const val PREVIEW_PIXEL = 1400
        const val VIDEO_ASPECT = 16f / 9f
        const val PHOTO_ASPECT = 3f / 4f
    }
}
