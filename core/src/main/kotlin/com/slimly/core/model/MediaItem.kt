package com.slimly.core.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant
import kotlin.time.Duration

enum class MediaKind {
    PHOTO,
    VIDEO,
}

/**
 * Файл медиатеки, отобранный для обработки.
 *
 * Размер храним в байтах: система отдаёт именно их, а мегабайты — уже
 * представление. `id` строковый, потому что в медиатеке это идентификатор
 * записи MediaStore, а не наш собственный ключ.
 */
@Parcelize
@TypeParceler<Duration?, DurationParceler>
@TypeParceler<Instant?, InstantParceler>
data class MediaItem(
    val id: String,
    val kind: MediaKind,
    val fileName: String,
    val byteCount: Long,
    /**
     * Вес файла, который реально уйдёт в обработку.
     *
     * У медиа с правками галерея хранит два файла: исходник с камеры и рендер
     * обрезки. `byteCount` — исходник, как его показывает системная галерея,
     * а обрабатываем мы рендер. Считать прогноз результата от исходника
     * нельзя: минутное видео, обрезанное до тридцати секунд, дало бы вдвое
     * завышенную оценку.
     */
    val sourceByteCount: Long = byteCount,
    /** Только у видео. */
    val duration: Duration? = null,
    /** HEIC можно переложить в JPEG — на вес это влияет слабо, на совместимость сильно. */
    val isHeic: Boolean = false,
    /**
     * Размер кадра: от него зависит, есть ли что уменьшать при сжатии,
     * и он же подписывает превью в сравнении «до / после».
     */
    val pixelWidth: Int = 0,
    val pixelHeight: Int = 0,
    /**
     * Файл, если элемент пришёл не из медиатеки: страница, снятая камерой,
     * записью MediaStore не является, и достать её по `id` неоткуда.
     */
    val sourceUri: Uri? = null,
    /**
     * Когда снят файл. По ней подписана сетка выбора: в медиатеке файлы идут
     * от свежих к старым, и дата — единственный ориентир при листании.
     * У страницы, снятой камерой, её нет: медиатеке она не принадлежит.
     */
    val creationDate: Instant? = null,
) : Parcelable {
    /** Имя без расширения — основа для имени результата. */
    val baseName: String
        get() = fileName.substringBeforeLast(delimiter = '.', missingDelimiterValue = fileName)

    val fileExtension: String
        get() = fileName.substringAfterLast(delimiter = '.', missingDelimiterValue = "").lowercase()

    /** «4032 × 3024». Пусто, если система размер не сообщила. */
    val pixelSizeText: String
        get() = if (pixelWidth > 0 && pixelHeight > 0) "$pixelWidth × $pixelHeight" else ""

    /** Длинная сторона кадра — по ней решаем, уменьшать ли изображение. */
    val longestSide: Int
        get() = maxOf(pixelWidth, pixelHeight)
}

val Iterable<MediaItem>.totalByteCount: Long
    get() = sumOf { it.byteCount }

val Iterable<MediaItem>.totalSourceByteCount: Long
    get() = sumOf { it.sourceByteCount }

/**
 * Альбом медиатеки.
 *
 * «Недавние» — не настоящий альбом, а вся выборка целиком: так же, как
 * в системном пикере, это первый пункт списка, а не отдельный режим.
 */
data class MediaAlbum(val id: String, val title: String, val itemCount: Int) {
    val isRecent: Boolean
        get() = id == RECENT_ID

    companion object {
        const val RECENT_ID = "recent"
    }
}

/**
 * Порция витрины: медиатеку читаем страницами, поэтому кроме файлов нужен
 * признак, осталось ли что грузить дальше. Считать его по длине страницы
 * нельзя: последняя порция может ровно совпасть с размером окна.
 */
data class MediaPage(val items: List<MediaItem>, val hasMore: Boolean) {
    companion object {
        val EMPTY = MediaPage(items = emptyList(), hasMore = false)
    }
}
