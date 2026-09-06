package com.slimly.mediakit.processing

import android.net.Uri
import com.slimly.core.model.ProcessingResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Смена имени у файла, который ещё лежит в рабочей папке. */
internal fun interface ProcessingResultRenaming {
    suspend fun rename(result: ProcessingResult, name: String): ProcessingResult
}

/**
 * Файл переезжает внутри своей папки, а не в соседнюю: уникальность даёт
 * папка, и переносить файл ради нового имени незачем — рядом всё равно
 * никого нет.
 */
internal class ProcessingResultRenamer(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) :
    ProcessingResultRenaming {
    override suspend fun rename(result: ProcessingResult, name: String): ProcessingResult = withContext(dispatcher) {
        val path = result.outputUri?.path ?: throw ProcessingError.OutputMissing()
        val file = File(path)
        val target = File(file.parentFile, name)

        if (target != file && !file.renameTo(target)) {
            throw ProcessingError.WriteFailed(name)
        }

        result.copy(outputName = name, outputUri = Uri.fromFile(target))
    }
}
