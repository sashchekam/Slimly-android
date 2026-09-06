package com.slimly.mediakit.camera

import android.media.ExifInterface
import android.net.Uri
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import java.io.File
import java.util.UUID

/** Снятые страницы на диске: куда положить кадр и чем он становится. */
internal interface CapturedPageStoring {
    /** Файл, в который камера положит следующий кадр. */
    fun makeFile(): File

    /** @param number номер страницы в наборе — он же станет именем файла. */
    fun page(file: File, number: Int): MediaItem

    fun discard(pages: List<MediaItem>)

    /** Убрать всё снятое раньше: набор начинается с чистой папки. */
    fun clear()
}

/**
 * Страница ложится во временную папку и дальше живёт как обычный исходник:
 * медиатеке она не принадлежит, поэтому носит с собой файл.
 */
internal class CapturedPages(private val root: File) : CapturedPageStoring {
    override fun makeFile(): File {
        root.mkdirs()
        return File(root, "${UUID.randomUUID()}.jpg")
    }

    override fun page(file: File, number: Int): MediaItem {
        val size = pixelSize(file)
        return MediaItem(
            id = file.path,
            kind = MediaKind.PHOTO,
            // Имя техническое и потому непереводимое: оно видно только
            // в списке страниц и в шторке «Поделиться».
            fileName = "Page $number.jpg",
            byteCount = file.length(),
            pixelWidth = size.first,
            pixelHeight = size.second,
            sourceUri = Uri.fromFile(file),
        )
    }

    override fun discard(pages: List<MediaItem>) {
        pages.forEach { page -> page.sourceUri?.path?.let { File(it).delete() } }
    }

    override fun clear() {
        root.deleteRecursively()
    }

    /**
     * Размер кадра берём из EXIF, а не из заголовка JPEG: снимок почти всегда
     * лежит в файле боком, и без разворота страница 3000 × 4000 показалась бы
     * альбомной.
     */
    private fun pixelSize(file: File): Pair<Int, Int> {
        val exif = runCatching { ExifInterface(file.path) }.getOrNull() ?: return 0 to 0
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val isSideways = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
            orientation == ExifInterface.ORIENTATION_ROTATE_270
        return if (isSideways) height to width else width to height
    }
}
