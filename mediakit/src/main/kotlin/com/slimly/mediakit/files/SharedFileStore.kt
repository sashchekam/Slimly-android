package com.slimly.mediakit.files

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.slimly.core.service.MediaLibraryError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Готовый файл в общей коллекции — туда, чему в галерее места нет. */
internal fun interface SharedFileStoring {
    /** Кладёт копию файла в общую коллекцию и отдаёт адрес созданной записи. */
    suspend fun store(file: File, name: String): Uri
}

/**
 * Документы и дорожки — в `Documents/Slimly` и `Music/Slimly`.
 *
 * Не папка приложения: `Android/data/…/files` с Android 11 закрыта для
 * системного менеджера файлов, и сохранённый туда PDF человеку уже
 * не достать. И не SAF: спрашивать папку ради своего же файла — лишний
 * шаг, а «Файлы» видят обе общие коллекции без спроса.
 *
 * Дорожка идёт в `MediaStore.Audio`, а не в `Files`, потому что иначе её
 * не найдёт ни один плеер: музыку они ищут в своей коллекции, а не обходом
 * диска.
 */
internal class SharedFileStore(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SharedFileStoring {
    override suspend fun store(file: File, name: String): Uri = withContext(dispatcher) {
        val mime = mimeType(name) ?: OCTET_STREAM
        val audio = mime.startsWith(AUDIO_PREFIX)
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${if (audio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_DOCUMENTS}/$FOLDER",
            )
            // До снятия отметки записи не видит никто: иначе «Файлы» успели бы
            // показать документ, дописанный до половины.
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val collection = if (audio) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val target = resolver.insert(collection, values)
            ?: throw MediaLibraryError.ExportFailed("MediaStore не завёл запись для $name")

        runCatching {
            resolver.openOutputStream(target).use { output ->
                requireNotNull(output) { "MediaStore не дал потока записи" }
                file.inputStream().use { it.copyTo(output) }
            }
        }.onFailure { error ->
            resolver.delete(target, null, null)
            throw MediaLibraryError.ExportFailed(error.message.orEmpty())
        }

        resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        target
    }

    private companion object {
        /** Своя папка в обеих коллекциях — как альбом «Slimly» в галерее. */
        const val FOLDER = "Slimly"
        const val AUDIO_PREFIX = "audio/"
    }
}
