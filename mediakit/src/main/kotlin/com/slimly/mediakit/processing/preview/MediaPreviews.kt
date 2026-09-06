package com.slimly.mediakit.processing.preview

import android.graphics.Bitmap
import android.net.Uri
import com.slimly.core.service.MediaPreviewRenderer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Кадр готового файла для экрана просмотра.
 *
 * Сам ничего не разбирает: выбирает читателя по расширению и уходит с ним
 * на исполнителя ввода-вывода. Расширение здесь — не догадка: имя готовому
 * файлу давали мы сами, и оно всегда соответствует записанному формату.
 */
class MediaPreviews internal constructor(
    private val images: OutputFraming,
    private val videos: OutputFraming,
    private val documents: OutputFraming,
    private val dispatcher: CoroutineDispatcher,
) : MediaPreviewRenderer {
    constructor() : this(
        images = ImageOutputFramer(),
        videos = VideoOutputFramer(),
        documents = DocumentOutputFramer(),
        dispatcher = Dispatchers.IO,
    )

    override suspend fun preview(uri: Uri, maxPixel: Int): Bitmap? {
        val path = uri.path ?: return null
        // У дорожки кадра нет вовсе — читателя для неё не найдётся, и это
        // не поломка: экран просмотра нарисует на её месте подпись.
        val framer = framer(uri) ?: return null

        return withContext(dispatcher) { framer.frame(File(path), maxPixel) }
    }

    private fun framer(uri: Uri): OutputFraming? =
        when (uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase()) {
            in IMAGE -> images
            in VIDEO -> videos
            in DOCUMENT -> documents
            else -> null
        }

    private companion object {
        val IMAGE = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
        val VIDEO = setOf("mp4", "m4v", "mov", "3gp", "webm", "mkv")
        val DOCUMENT = setOf("pdf")
    }
}
