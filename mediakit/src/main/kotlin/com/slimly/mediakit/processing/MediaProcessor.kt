package com.slimly.mediakit.processing

import android.content.Context
import com.slimly.core.debug.SimulatedFailureProviding
import com.slimly.core.model.ProcessingEvent
import com.slimly.core.model.ProcessingFailure
import com.slimly.core.model.ProcessingHistory
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import com.slimly.core.model.ProcessingRun
import com.slimly.core.model.ProcessingStats
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaProcessingService
import com.slimly.core.service.ProcessingStatsStore
import com.slimly.mediakit.files.SharedFileStore
import com.slimly.mediakit.processing.foreground.ForegroundWork
import com.slimly.mediakit.processing.video.VideoTranscoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Настоящая обработка файлов.
 *
 * Сам ничего не кодирует и не пишет: раздаёт работу воркерам — держателю
 * идущей очереди и хранилищу результата.
 */
class MediaProcessor internal constructor(
    private val statsStore: ProcessingStatsStore,
    private val output: OutputStoring,
    private val runs: ProcessingRunning,
    private val messages: FailureMessaging,
    private val renames: ProcessingResultRenaming,
    workFiles: WorkFileProviding,
) : MediaProcessingService {
    init {
        workFiles.clear()
    }

    override suspend fun stats(): ProcessingStats = statsStore.stats()

    override suspend fun history(): ProcessingHistory = statsStore.history()

    override val active: StateFlow<ProcessingRun?>
        get() = runs.active

    override fun process(request: ProcessingRequest): Flow<ProcessingEvent> = runs.events(request)

    override fun cancel() {
        runs.cancel()
    }

    /**
     * Вынести готовые файлы наружу — по кнопке на экране итога.
     *
     * Здесь же учитывается статистика: до сохранения ничего не освобождено
     * и никуда не положено, а профиль считает сделанное, а не проделанное.
     */
    override suspend fun save(outcome: ProcessingOutcome): ProcessingOutcome {
        val saved = mutableListOf<ProcessingResult>()
        val failures = outcome.failures.toMutableList()

        outcome.results.forEach { result ->
            try {
                val file = result.outputUri ?: throw ProcessingError.OutputMissing()
                output.store(
                    file = file,
                    kind = result.item.kind,
                    name = result.outputName,
                    source = result.item,
                    destination = result.destination,
                )
                saved += result
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                // Как и в очереди: один несохранившийся файл не отменяет
                // остальные, а причина уходит в список отказов.
                failures += ProcessingFailure(item = result.item, message = messages.message(error))
            }
        }

        val stored = ProcessingOutcome(request = outcome.request, results = saved, failures = failures)
        if (saved.isNotEmpty()) {
            statsStore.add(stored)
        }
        return stored
    }

    override suspend fun rename(result: ProcessingResult, name: String): ProcessingResult = renames.rename(result, name)

    companion object {
        /**
         * Состав по умолчанию: всё, что нужно снаружи, — медиатека, из которой
         * приходят оригиналы и в которую возвращается результат.
         *
         * @param simulatedFailure подмена отказа из отладочного меню.
         *   По умолчанию её нет: релизная сборка меню не собирает вовсе.
         */
        fun of(
            context: Context,
            library: MediaLibrary,
            statsStore: ProcessingStatsStore,
            simulatedFailure: SimulatedFailureProviding = SimulatedFailureProviding { null },
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): MediaProcessor {
            val workFiles = WorkDirectory(File(context.cacheDir, OUTPUT_FOLDER))
            val messages = FailureMessages(context.resources)
            val results = ProcessingResultWriter(workFiles)
            val videos = VideoTranscoder(context)

            return MediaProcessor(
                statsStore = statsStore,
                output = OutputStore(library, SharedFileStore(context, dispatcher)),
                runs = ProcessingRunner(
                    queue = ProcessingQueues(
                        items = ProcessingQueue(
                            items = MediaItemProcessor(
                                library = library,
                                compression = CompressionWorker(videos = videos, results = results),
                                conversion = ConversionWorker(videos = videos, results = results),
                                audio = AudioWorker(results = results),
                            ),
                            messages = messages,
                        ),
                        documents = ScanAssembler(
                            library = library,
                            names = ScanDocumentNamer(context.resources),
                            results = results,
                            messages = messages,
                        ),
                        failures = simulatedFailure,
                    ),
                    foreground = ForegroundWork(context),
                    dispatcher = dispatcher,
                ),
                messages = messages,
                renames = ProcessingResultRenamer(dispatcher),
                workFiles = workFiles,
            )
        }

        private const val OUTPUT_FOLDER = "slimly-output"
    }
}
