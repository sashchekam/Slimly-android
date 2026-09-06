package com.slimly.mediakit.processing.video

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.slimly.mediakit.processing.ProcessingError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Запуск задания и ожидание его конца. */
internal fun interface VideoExportRunning {
    suspend fun run(transformer: Transformer, item: EditedMediaItem, destination: File, progress: (Double) -> Unit)
}

/**
 * Transformer живёт на потоке с очередью сообщений: и запуск, и опрос
 * прогресса, и отмена должны идти оттуда же, откуда он собран. Поэтому вся
 * работа с ним идёт на главном потоке — само перекодирование Transformer
 * всё равно уводит на свои.
 *
 * Прогресс приходится спрашивать самим: события о нём Transformer не шлёт.
 */
@OptIn(UnstableApi::class)
internal class VideoExportRunner : VideoExportRunning {
    override suspend fun run(
        transformer: Transformer,
        item: EditedMediaItem,
        destination: File,
        progress: (Double) -> Unit,
    ) = withContext(Dispatchers.Main) {
        coroutineScope {
            val ticker = launch { report(transformer, progress) }
            try {
                await(transformer, item, destination)
            } finally {
                ticker.cancel()
            }
        }
    }

    private suspend fun await(transformer: Transformer, item: EditedMediaItem, destination: File) =
        suspendCancellableCoroutine { continuation ->
            transformer.addListener(
                object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        continuation.resume(Unit)
                    }

                    override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                        continuation.resumeWithException(
                            ProcessingError.WriteFailed(exception.message.orEmpty(), exception),
                        )
                    }
                },
            )

            continuation.invokeOnCancellation {
                // Отмену зовут с того потока, который отменяли, — а Transformer
                // слушает только свой. Недописанный файл убирает за собой тот,
                // кто его завёл.
                Handler(Looper.getMainLooper()).post { transformer.cancel() }
            }

            transformer.start(item, destination.path)
        }

    private suspend fun report(transformer: Transformer, progress: (Double) -> Unit) {
        val holder = ProgressHolder()

        while (true) {
            if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                progress(holder.progress / PERCENT)
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private companion object {
        /**
         * Чаще спрашивать незачем: Transformer считает прогресс по кадрам
         * и между опросами он всё равно почти не меняется.
         */
        const val POLL_INTERVAL_MS = 200L
        const val PERCENT = 100.0
    }
}
