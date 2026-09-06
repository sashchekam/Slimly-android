package com.slimly.mediakit.library

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.slimly.core.model.MediaAlbum
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.MediaPage
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Чтение витрины из MediaStore.
 *
 * Коллекции фото и видео читаются по отдельности, а не одним запросом
 * к `MediaStore.Files`: разрешения с Android 13 выдаются на фото и на видео
 * порознь, и общая таблица под ними ведёт себя по-разному на разных версиях.
 * Когда инструменту нужны оба вида, страницы просто сливаются по дате.
 */
internal class MediaStoreCursors(private val resolver: ContentResolver, private val recentTitle: String) {
    fun albums(kind: MediaKind?): List<MediaAlbum> {
        val buckets = LinkedHashMap<String, Album>()
        var total = 0

        collections(kind).forEach { collection ->
            resolver.query(collection.uri, BUCKET_COLUMNS, null, null, SORT_ORDER)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    total++
                    val id = cursor.getString(idColumn) ?: continue
                    val album = buckets.getOrPut(id) { Album(id, cursor.getString(nameColumn).orEmpty()) }
                    album.count++
                }
            }
        }

        // «Недавние» — не настоящий альбом, а вся выборка целиком: так же,
        // как в системном пикере, это первый пункт списка.
        val recent = MediaAlbum(MediaAlbum.RECENT_ID, recentTitle, total)
        return listOf(recent) + buckets.values
            .filter { it.title.isNotEmpty() }
            .sortedByDescending { it.count }
            .map { MediaAlbum(it.id, it.title, it.count) }
    }

    fun page(albumId: String, kind: MediaKind?, offset: Int, limit: Int): MediaPage {
        val collections = collections(kind)
        if (collections.size == 1) return page(collections[0], albumId, offset, limit)

        // Два вида сразу: страницу собираем слиянием. Читать приходится
        // от начала обеих коллекций — окно в объединённом порядке иначе
        // не вырезать, а курсоров с общим порядком MediaStore не даёт.
        val merged = collections
            .flatMap { page(it, albumId, offset = 0, limit = offset + limit + 1).items }
            .sortedByDescending { it.creationDate ?: Instant.EPOCH }

        return MediaPage(
            items = merged.drop(offset).take(limit),
            hasMore = merged.size > offset + limit,
        )
    }

    private fun page(collection: Collection, albumId: String, offset: Int, limit: Int): MediaPage {
        val isRecent = albumId == MediaAlbum.RECENT_ID
        val selection = if (isRecent) null else "${MediaStore.MediaColumns.BUCKET_ID} = ?"
        val arguments = if (isRecent) null else arrayOf(albumId)
        // Одну лишнюю запись читаем нарочно: по ней видно, есть ли что
        // грузить дальше. Считать это по длине страницы нельзя — последняя
        // порция может ровно совпасть с размером окна.
        val window = limit + 1

        val cursor = query(collection.uri, selection, arguments, offset, window) ?: return MediaPage.EMPTY

        val items = cursor.use { it.items(collection) }
        return MediaPage(items = items.take(limit), hasMore = items.size > limit)
    }

    private fun query(uri: Uri, selection: String?, arguments: Array<String>?, offset: Int, limit: Int): Cursor? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val extras = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arguments)
                putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, SORT_ORDER)
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            resolver.query(uri, COLUMNS, extras, null)
        } else {
            // На Android 10 окна запроса ещё нет — предел дописывается
            // в порядок сортировки. MediaStore его понимает.
            resolver.query(uri, COLUMNS, selection, arguments, "$SORT_ORDER LIMIT $limit OFFSET $offset")
        }

    private fun Cursor.items(collection: Collection): List<MediaItem> {
        val id = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val name = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val size = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val mime = getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        // Эти столбцы у части записей и у части прошивок не заводятся вовсе —
        // спрашиваем мягко: витрина без размера кадра работает, а с падением
        // курсора нет.
        val width = getColumnIndex(MediaStore.MediaColumns.WIDTH)
        val height = getColumnIndex(MediaStore.MediaColumns.HEIGHT)
        val duration = getColumnIndex(MediaStore.MediaColumns.DURATION)
        val taken = getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
        val added = getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)

        return buildList {
            while (moveToNext()) {
                val uri = ContentUris.withAppendedId(collection.uri, getLong(id))
                val type = getString(mime).orEmpty()

                add(
                    MediaItem(
                        // Идентификатор — адрес записи целиком: номера
                        // у фото и видео нумеруются порознь и совпадают.
                        id = uri.toString(),
                        kind = collection.kind,
                        fileName = getString(name).orEmpty(),
                        byteCount = getLong(size),
                        duration = number(duration).takeIf { it > 0 }?.milliseconds,
                        isHeic = type in HEIC_TYPES,
                        pixelWidth = number(width).toInt(),
                        pixelHeight = number(height).toInt(),
                        sourceUri = uri,
                        creationDate = date(taken = number(taken), added = number(added)),
                    ),
                )
            }
        }
    }

    /** `-1` — столбца в курсоре нет: значения просто не будет. */
    private fun Cursor.number(column: Int): Long = if (column >= 0 && !isNull(column)) getLong(column) else 0

    /**
     * Дата съёмки, а не появления в галерее: скачанный файл добавлен сегодня,
     * а снят год назад, и в ленте он должен стоять по съёмке. Съёмку система
     * знает не всегда — тогда остаётся дата добавления.
     */
    private fun date(taken: Long, added: Long): Instant? = when {
        taken > 0 -> Instant.ofEpochMilli(taken)
        added > 0 -> Instant.ofEpochSecond(added)
        else -> null
    }

    private fun collections(kind: MediaKind?): List<Collection> = when (kind) {
        MediaKind.PHOTO -> listOf(PHOTOS)
        MediaKind.VIDEO -> listOf(VIDEOS)
        null -> listOf(PHOTOS, VIDEOS)
    }

    private data class Collection(val uri: Uri, val kind: MediaKind)

    private class Album(val id: String, val title: String, var count: Int = 0)

    private companion object {
        val PHOTOS = Collection(
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaKind.PHOTO,
        )
        val VIDEOS = Collection(
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
            MediaKind.VIDEO,
        )

        val COLUMNS = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
        )

        val BUCKET_COLUMNS = arrayOf(
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        )

        /**
         * Свежие сверху — так же, как в системной галерее.
         *
         * По дате добавления, а не съёмки: снимок камеры знает обе, а всё
         * скачанное и присланное — только эту, и по дате съёмки половина
         * галереи уехала бы в самый низ.
         */
        const val SORT_ORDER = "${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"

        val HEIC_TYPES = setOf("image/heic", "image/heif", "image/heic-sequence")
    }
}
