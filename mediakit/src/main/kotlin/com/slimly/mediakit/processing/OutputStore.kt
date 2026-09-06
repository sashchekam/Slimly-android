package com.slimly.mediakit.processing

import android.net.Uri
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.OutputDestination
import com.slimly.core.service.MediaLibrary
import com.slimly.mediakit.files.SharedFileStoring
import java.io.File

/**
 * Куда попадает сохранённый файл.
 *
 * Фото и видео уходят в галерею — там их ищут. Звуку и документам в галерее
 * места нет, и они ложатся в общие коллекции: PDF в `Documents/Slimly`,
 * дорожка в `Music/Slimly`. И то и другое «Файлы» показывают без спроса.
 *
 * Само раскладывание происходит только по кнопке на экране итога: до неё
 * результат не покидает рабочую папку. Копия в ней остаётся и после —
 * ею делятся и по ней показывают «после» в сравнении.
 */
internal fun interface OutputStoring {
    suspend fun store(
        file: Uri,
        kind: MediaKind,
        name: String,
        source: MediaItem?,
        destination: OutputDestination,
    ): Uri
}

internal class OutputStore(private val library: MediaLibrary, private val files: SharedFileStoring) : OutputStoring {
    override suspend fun store(
        file: Uri,
        kind: MediaKind,
        name: String,
        source: MediaItem?,
        destination: OutputDestination,
    ): Uri = when (destination) {
        OutputDestination.GALLERY -> library.saveToLibrary(file, kind, source)

        OutputDestination.FILES -> files.store(
            file = File(requireNotNull(file.path) { "Сохранять нечего: у файла нет пути" }),
            name = name,
        )
    }
}
