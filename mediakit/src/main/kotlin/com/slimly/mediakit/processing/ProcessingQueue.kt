package com.slimly.mediakit.processing

import com.slimly.core.debug.SimulatedFailureProviding
import com.slimly.core.model.ProcessingFailure
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import kotlinx.coroutines.CancellationException

/** Очередь файлов одного задания. */
internal fun interface ProcessingQueueing {
    suspend fun run(request: ProcessingRequest, progress: ProgressReporting): ProcessingOutcome
}

/**
 * Файл за файлом: у каждого исходника получается свой результат.
 *
 * Файлы идут по очереди, а не разом: кодировщик у устройства один,
 * параллельная очередь только отняла бы у него память, а прогресс превратила
 * бы в дрожание нескольких полос сразу.
 */
internal class ProcessingQueue(private val items: ItemProcessing, private val messages: FailureMessaging) :
    ProcessingQueueing {
    override suspend fun run(request: ProcessingRequest, progress: ProgressReporting): ProcessingOutcome {
        val results = mutableListOf<ProcessingResult>()
        val failures = mutableListOf<ProcessingFailure>()

        request.items.forEachIndexed { index, item ->
            try {
                results += items.process(item, request.operation) { progress.update(index, it) }
            } catch (cancelled: CancellationException) {
                // Отмену несём выше: с экрана ушли, и итог никому не нужен.
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                // Ловим всё подряд намеренно: один битый файл не должен
                // обрывать очередь — остальные обработаются, а про этот
                // честно скажем на экране итога. Заранее перечислить,
                // чем именно ответит кодировщик чужого устройства, нельзя.
                failures += ProcessingFailure(item = item, message = messages.message(error))
            }
            progress.update(index, 1.0)
        }

        return ProcessingOutcome(request = request, results = results, failures = failures)
    }
}

/**
 * Куда попадает задание.
 *
 * Развилка одна на всю обработку: у скана из пачки страниц получается один
 * документ, у остальных инструментов — файл на каждый исходник. Дальше
 * очереди не знают друг о друге.
 *
 * Здесь же единственная точка, где отладочное меню может подменить работу
 * отказом: обе очереди проходят через неё, и второй копии проверки
 * не понадобилось.
 */
internal class ProcessingQueues(
    private val items: ProcessingQueueing,
    private val documents: ProcessingQueueing,
    private val failures: SimulatedFailureProviding,
) : ProcessingQueueing {
    override suspend fun run(request: ProcessingRequest, progress: ProgressReporting): ProcessingOutcome {
        failures.reason()?.let { return refused(request, it, progress) }

        return if (request.operation is ProcessingOperation.Scan) {
            documents.run(request, progress)
        } else {
            items.run(request, progress)
        }
    }

    /**
     * Подменённый отказ: ни один файл не трогаем, каждый уезжает в список
     * отказов с выбранной причиной. Прогресс всё равно доводим до конца —
     * экран обработки ждёт своей сотни процентов, а не обрыва.
     */
    private fun refused(request: ProcessingRequest, reason: String, progress: ProgressReporting): ProcessingOutcome {
        request.items.indices.forEach { progress.update(it, 1.0) }

        return ProcessingOutcome(
            request = request,
            results = emptyList(),
            failures = request.items.map { ProcessingFailure(item = it, message = reason) },
        )
    }
}
