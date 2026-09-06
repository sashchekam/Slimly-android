package com.slimly.mediakit.processing

import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.MediaItem
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingFailure
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import com.slimly.core.model.ProcessingTarget
import com.slimly.core.model.ScanMode
import com.slimly.core.service.MediaLibrary
import com.slimly.mediakit.scan.DocumentWriting
import com.slimly.mediakit.scan.PdfDocumentWriter
import com.slimly.mediakit.scan.ScanPageRenderer
import com.slimly.mediakit.scan.ScanPageRendering
import com.slimly.mediakit.scan.ScannedPage
import kotlinx.coroutines.CancellationException
import java.io.File

/**
 * Сборка пачки снимков в один документ.
 *
 * Скан идёт своей дорогой: из пачки страниц получается один файл, а не файл
 * на каждый, — и поштучная очередь ему не годится. Отсюда и общий с ней
 * интерфейс: снаружи это такая же работа над заданием, просто с другим
 * устройством внутри.
 *
 * Нечитаемая страница документ не отменяет: соберём PDF из тех, что
 * получились, а про пропущенные скажем на экране итога.
 */
internal class ScanAssembler(
    private val library: MediaLibrary,
    private val names: DocumentNaming,
    private val results: ProcessingResultWriting,
    private val messages: FailureMessaging,
    private val pages: ScanPageRendering = ScanPageRenderer(),
    private val documents: DocumentWriting = PdfDocumentWriter(),
) : ProcessingQueueing {
    override suspend fun run(request: ProcessingRequest, progress: ProgressReporting): ProcessingOutcome {
        val mode = (request.operation as? ProcessingOperation.Scan)
            ?.let { (it.format.target as? ProcessingTarget.Document)?.mode }
            ?: return refused(request, ProcessingError.UnsupportedOperation())

        val bounds = (request.operation as ProcessingOperation.Scan).bounds
        val prepared = mutableListOf<Pair<MediaItem, ScannedPage>>()
        val failures = mutableListOf<ProcessingFailure>()

        request.items.forEachIndexed { index, item ->
            try {
                prepared += item to page(item, mode, bounds[item])
                progress.update(index, PAGE_SHARE)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                failures += ProcessingFailure(item = item, message = messages.message(error))
                progress.update(index, 1.0)
            }
        }

        return document(request, prepared, failures, progress)
    }

    /**
     * @param cover первая удавшаяся страница. Результат у скана один на весь
     *   заказ, а [ProcessingResult] привязан к исходнику — берём ту страницу,
     *   что открывается в документе первой: её превью и увидят в итоге.
     */
    private suspend fun document(
        request: ProcessingRequest,
        prepared: List<Pair<MediaItem, ScannedPage>>,
        failures: List<ProcessingFailure>,
        progress: ProgressReporting,
    ): ProcessingOutcome {
        val cover = prepared.firstOrNull()?.first
            ?: return ProcessingOutcome(request = request, results = emptyList(), failures = failures)

        return try {
            val result = assemble(prepared.map { it.second }, cover)
            request.items.indices.forEach { progress.update(it, 1.0) }
            ProcessingOutcome(request = request, results = listOf(result), failures = failures)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            // Документ не собрался — значит, не вышло ни одной страницы:
            // по отдельности они никуда не сохраняются.
            val reason = messages.message(error)
            val lost = prepared.map { ProcessingFailure(item = it.first, message = reason) }
            ProcessingOutcome(request = request, results = emptyList(), failures = failures + lost)
        }
    }

    /** Документ ложится в «Файлы»: PDF в галерее не место, да её он и не примет. */
    private suspend fun assemble(prepared: List<ScannedPage>, cover: MediaItem): ProcessingResult {
        val name = names.name()
        return results.write(name = name, item = cover, destination = OutputDestination.FILES) { file ->
            documents.write(prepared, file, name.substringBeforeLast('.'))
        }
    }

    private suspend fun page(item: MediaItem, mode: ScanMode, quad: DocumentQuad?): ScannedPage {
        val source = library.exportOriginal(item)
        val file = File(requireNotNull(source.path) { "Копия оригинала без пути" })
        try {
            return pages.page(file, mode, quad)
        } finally {
            // Копию оригинала за нами никто не уберёт: система чистит кэш
            // только когда на устройстве кончается место.
            file.delete()
        }
    }

    private fun refused(request: ProcessingRequest, error: ProcessingError): ProcessingOutcome = ProcessingOutcome(
        request = request,
        results = emptyList(),
        failures = request.items.map { ProcessingFailure(item = it, message = messages.message(error)) },
    )

    private companion object {
        /**
         * Доля прогресса страницы, приходящаяся на её подготовку. Остаток —
         * сборка PDF: она одна на весь документ, и до её конца ни одна
         * страница по-настоящему не готова.
         */
        const val PAGE_SHARE = 0.9
    }
}
