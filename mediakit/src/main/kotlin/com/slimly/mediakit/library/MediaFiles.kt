package com.slimly.mediakit.library

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.service.MediaLibraryError
import com.slimly.mediakit.files.mimeType
import java.io.File

/**
 * Файловая сторона медиатеки: копия оригинала наружу, готовый файл внутрь
 * и запрос на удаление.
 *
 * Отдельно от чтения витрины: витрина спрашивает базу, а здесь идёт работа
 * с байтами, и ломается она по другим причинам.
 */
internal class MediaFiles(private val context: Context) {
    private val resolver: ContentResolver get() = context.contentResolver

    /**
     * Копия оригинала во временной папке — источник для обработки.
     *
     * Копия, а не адрес записи: кодировщику нужен обычный файл, а `content://`
     * умеет отдавать только поток, и перематывать его назад нельзя.
     */
    fun exportOriginal(item: MediaItem): Uri {
        val source = item.sourceUri ?: throw MediaLibraryError.ItemNotFound()
        val folder = File(context.cacheDir, SOURCE_FOLDER).apply { mkdirs() }
        val file = File(folder, "${System.nanoTime()}-${item.fileName}")

        val stream = runCatching { resolver.openInputStream(source) }.getOrNull()
            ?: throw MediaLibraryError.OriginalUnavailable()

        stream.use { input ->
            file.outputStream().use(input::copyTo)
        }
        return Uri.fromFile(file)
    }

    /**
     * Кадр файла, которого в медиатеке нет: страницу, снятую камерой,
     * система по `content://` не отдаст, и готовой миниатюры у неё
     * не заведено.
     *
     * Уменьшаем при разборе: поднимать в память двенадцать мегапикселей ради
     * плитки незачем, а `ImageDecoder` заодно разворачивает кадр по EXIF —
     * снимок с камеры почти всегда лежит в файле боком.
     */
    fun thumbnail(uri: Uri, size: Int): Bitmap? {
        val path = uri.path ?: return null
        return runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(path))) { decoder, info, _ ->
                val side = maxOf(info.size.width, info.size.height)
                decoder.setTargetSampleSize(maxOf(1, side / size.coerceAtLeast(1)))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull()
    }

    /**
     * Положить готовый файл в галерею.
     *
     * Запись сначала помечается недописанной: до снятия отметки её не видят
     * ни галерея, ни другие приложения — иначе они успели бы показать файл
     * в половину скачанного.
     */
    fun saveToLibrary(file: Uri, kind: MediaKind, source: MediaItem?): Uri {
        val path = File(requireNotNull(file.path) { "Сохранять нечего: у файла нет пути" })
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, path.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType(path.name) ?: defaultType(kind))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${directory(kind)}/$ALBUM")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            // Дата съёмки переезжает на результат: иначе пережатое фото
            // встанет в конец галереи сегодняшним числом, а не рядом
            // с оригиналом.
            source?.creationDate?.let { put(MediaStore.MediaColumns.DATE_TAKEN, it.toEpochMilli()) }
        }

        val collection = when (kind) {
            MediaKind.PHOTO -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            MediaKind.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val target = resolver.insert(collection, values)
            ?: throw MediaLibraryError.ExportFailed("MediaStore не завёл запись для ${path.name}")

        runCatching {
            resolver.openOutputStream(target).use { output ->
                requireNotNull(output) { "MediaStore не дал потока записи" }
                path.inputStream().use { it.copyTo(output) }
            }
        }.onFailure { error ->
            // Недописанная запись не должна пережить попытку: иначе
            // в галерее останется пустой файл, который ничем не открыть.
            resolver.delete(target, null, null)
            throw MediaLibraryError.ExportFailed(error.message.orEmpty())
        }

        resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return target
    }

    /**
     * Запрос на удаление оригиналов.
     *
     * С Android 11 система собирает подтверждение сама и отдаёт намерение;
     * до неё — отказывает на чужом файле и прикладывает к отказу тот же
     * запрос. Показать его в обоих случаях может только Activity.
     */
    fun deleteRequest(items: List<MediaItem>): IntentSender? {
        val uris = items.mapNotNull { it.sourceUri }
        return when {
            uris.isEmpty() -> null

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                MediaStore.createDeleteRequest(resolver, uris).intentSender

            else -> deleteEach(uris)
        }
    }

    /**
     * До Android 11 общего запроса нет: система отказывает на первом же чужом
     * файле и прикладывает к отказу своё подтверждение — его и показываем.
     */
    private fun deleteEach(uris: List<Uri>): IntentSender? {
        uris.forEach { uri ->
            try {
                resolver.delete(uri, null, null)
            } catch (denied: RecoverableSecurityException) {
                return denied.userAction.actionIntent.intentSender
            }
        }
        return null
    }

    private fun directory(kind: MediaKind): String = when (kind) {
        MediaKind.PHOTO -> Environment.DIRECTORY_PICTURES
        MediaKind.VIDEO -> Environment.DIRECTORY_MOVIES
    }

    /**
     * Тип на случай незнакомого расширения. Взяться ему тут неоткуда — имя
     * файлу давали мы сами, — но без типа галерея запись не заводит вовсе.
     */
    private fun defaultType(kind: MediaKind): String = when (kind) {
        MediaKind.PHOTO -> "image/jpeg"
        MediaKind.VIDEO -> "video/mp4"
    }

    private companion object {
        /** Куда результат ложится в галерее — своим альбомом, а не в общий ворох. */
        const val ALBUM = "Slimly"
        const val SOURCE_FOLDER = "slimly-source"
    }
}
