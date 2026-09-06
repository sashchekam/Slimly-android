package com.slimly.mediakit.processing

import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingResult
import com.slimly.core.service.MediaLibrary
import java.io.File

/** Обработка одного файла: от оригинала из медиатеки до готового результата. */
internal fun interface ItemProcessing {
    suspend fun process(item: MediaItem, operation: ProcessingOperation, progress: (Double) -> Unit): ProcessingResult
}

/**
 * Сам ничего не кодирует: достаёт оригинал и отдаёт его тому воркеру,
 * который занимается этой операцией.
 */
internal class MediaItemProcessor(
    private val library: MediaLibrary,
    private val compression: CompressionPerforming,
    private val conversion: ConversionPerforming,
    private val audio: AudioExtracting,
) : ItemProcessing {
    override suspend fun process(
        item: MediaItem,
        operation: ProcessingOperation,
        progress: (Double) -> Unit,
    ): ProcessingResult {
        val source = library.exportOriginal(item)

        try {
            return when (operation) {
                is ProcessingOperation.Compress -> compression.compress(item, source, operation.level, progress)

                is ProcessingOperation.Convert -> conversion.convert(item, source, operation.format, progress)

                is ProcessingOperation.ExtractAudio -> audio.extract(item, source, operation.format, progress)

                // Скан собирается не поштучно: страница становится PDF
                // вместе со всем документом, и сюда не попадает.
                else -> throw ProcessingError.UnsupportedOperation()
            }
        } finally {
            // Копию оригинала за нами никто не уберёт: система чистит кэш
            // только когда на устройстве кончается место.
            source.path?.let { File(it).delete() }
        }
    }
}
