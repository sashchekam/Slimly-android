package com.slimly.mediakit.library

import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.net.Uri
import android.os.StatFs
import android.util.Size
import com.slimly.core.model.LibrarySnapshot
import com.slimly.core.model.MediaAlbum
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.MediaPage
import com.slimly.core.service.MediaAccess
import com.slimly.core.service.MediaLibrary
import com.slimly.mediakit.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Медиатека устройства на MediaStore.
 *
 * Сама почти ничего не делает: спрашивает у системы разрешения, читает
 * витрину курсорами и работает с файлами — три разные работы, и у каждой
 * свой воркер. Здесь остаётся только то, ради чего фасад и заводят:
 * увести всё это с главного потока.
 */
class MediaStoreLibrary(private val context: Context, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) :
    MediaLibrary {
    private val cursors = MediaStoreCursors(context.contentResolver, context.getString(R.string.album_recent))
    private val files = MediaFiles(context)

    override fun permissions(kind: MediaKind?): List<String> = MediaPermissions.requested(kind)

    override suspend fun access(): MediaAccess = MediaPermissions.access(context, kind = null)

    /**
     * Свободное место система знает, а вес медиатеки — нет: суммировать его
     * пришлось бы обходом всех записей, и на большой галерее это заметная
     * пауза ради цифры, которую никто не просил.
     */
    override suspend fun snapshot(): LibrarySnapshot = withContext(dispatcher) {
        val stats = StatFs(context.filesDir.absolutePath)
        LibrarySnapshot(
            photoCount = cursors.albums(MediaKind.PHOTO).firstOrNull()?.itemCount ?: 0,
            videoCount = cursors.albums(MediaKind.VIDEO).firstOrNull()?.itemCount ?: 0,
            freeSpace = stats.availableBytes,
        )
    }

    override suspend fun albums(kind: MediaKind?): List<MediaAlbum> = withContext(dispatcher) {
        cursors.albums(kind)
    }

    override suspend fun items(albumId: String, kind: MediaKind?, offset: Int, limit: Int): MediaPage =
        withContext(dispatcher) { cursors.page(albumId, kind, offset, limit) }

    /**
     * Миниатюру собирает сама система: она держит их кэш и отдаёт готовый
     * кадр, не разбирая ради него весь файл.
     */
    override suspend fun thumbnail(item: MediaItem, size: Int): Bitmap? = withContext(dispatcher) {
        val uri = item.sourceUri ?: return@withContext null
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            // Страница, снятая камерой, записью MediaStore не является:
            // кэша миниатюр для неё у системы нет, и кадр мы берём сами.
            files.thumbnail(uri, size)
        } else {
            runCatching { context.contentResolver.loadThumbnail(uri, Size(size, size), null) }.getOrNull()
        }
    }

    override suspend fun exportOriginal(item: MediaItem): Uri = withContext(dispatcher) {
        files.exportOriginal(item)
    }

    override suspend fun playbackUri(item: MediaItem): Uri? = item.sourceUri

    override suspend fun saveToLibrary(file: Uri, kind: MediaKind, source: MediaItem?): Uri =
        withContext(dispatcher) { files.saveToLibrary(file, kind, source) }

    override suspend fun deleteRequest(items: List<MediaItem>): IntentSender? = withContext(dispatcher) {
        files.deleteRequest(items)
    }
}
