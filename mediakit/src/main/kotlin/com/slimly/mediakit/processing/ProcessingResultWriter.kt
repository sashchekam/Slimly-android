package com.slimly.mediakit.processing

import android.net.Uri
import com.slimly.core.model.MediaItem
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingResult
import java.io.File

/** Запись результата в рабочую папку. */
internal fun interface ProcessingResultWriting {
    suspend fun write(
        name: String,
        item: MediaItem,
        destination: OutputDestination,
        body: suspend (File) -> Unit,
    ): ProcessingResult
}

/**
 * Недописанный файл после ошибки или отмены не должен пережить попытку:
 * иначе кэш копит куски, которые ни показать, ни сохранить.
 */
internal class ProcessingResultWriter(private val workFiles: WorkFileProviding) : ProcessingResultWriting {
    override suspend fun write(
        name: String,
        item: MediaItem,
        destination: OutputDestination,
        body: suspend (File) -> Unit,
    ): ProcessingResult {
        val file = workFiles.makeFile(name)

        try {
            body(file)
        } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
            // Убираем за собой на любом исходе, включая отмену: недописанный
            // файл ни показать, ни сохранить, а место он занимает.
            file.delete()
            throw error
        }

        return ProcessingResult(
            item = item,
            outputByteCount = file.length(),
            outputName = name,
            outputUri = Uri.fromFile(file),
            destination = destination,
        )
    }
}
