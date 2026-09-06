package com.slimly.mediaflow.run

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.ItemProgress
import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingEvent
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingProgress
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.service.MediaProcessingService
import com.slimly.mediaflow.FlowAnalytics
import com.slimly.mediaflow.FlowCopy
import com.slimly.mediaflow.MediaThumbnailLoader
import com.slimly.navigation.Route
import com.slimly.navigation.Router
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class ProcessingViewModel(
    val request: ProcessingRequest,
    val thumbnails: MediaThumbnailLoader,
    private val service: MediaProcessingService,
    private val formatters: SlimlyFormatters,
    private val copy: FlowCopy,
    private val analytics: FlowAnalytics,
    private val router: Router,
    private val outcome: (ProcessingOutcome) -> Route,
) {
    var progress: ProcessingProgress by mutableStateOf(ProcessingProgress.idle(request.items))
        private set

    val items: List<MediaItem>
        get() = request.items

    /**
     * Подписка на уже идущую работу. Обработка живёт в приложении, а не
     * в этой подписке: свернуть приложение или потерять экран на повороте
     * работа переживает, и повторный вход подхватывает прогресс с того же
     * места. Останавливает её только отмена.
     */
    suspend fun run() {
        val startedAt = System.currentTimeMillis()
        // Старт пишем здесь, а не на кнопке предыдущего шага: у сжатия
        // её нажимают в шторке, и точка запуска у инструментов разъехалась
        // бы, хотя работа начинается в одном и том же месте.
        analytics.didStartRun(request)

        service.process(request).collect { event ->
            when (event) {
                is ProcessingEvent.Progress ->
                    progress = paced(event.progress, System.currentTimeMillis() - startedAt)

                is ProcessingEvent.Finished -> finish(event.outcome, startedAt)
            }
        }
    }

    private suspend fun finish(result: ProcessingOutcome, startedAt: Long) {
        // Время меряем до докрутки кольца: докрутка — это показ, а не работа,
        // и в отчёте она превратила бы любую обработку в двухсекундную.
        analytics.didFinishRun(result, (System.currentTimeMillis() - startedAt).milliseconds)
        settle(startedAt)
        // Экран обработки из истории убираем: возврат с итога должен вести
        // на хаб, а не запускать всё заново.
        router.changePath(listOf(outcome(result)))
    }

    /**
     * Докручивает кольцо до конца за остаток минимального времени: иначе
     * последним кадром был бы скачок с 20% сразу на экран итога.
     */
    private suspend fun settle(startedAt: Long) {
        val full = ProcessingProgress(
            overall = 1.0,
            items = request.items.map { ItemProgress(it.id, fraction = 1.0) },
            estimatedRemaining = null,
        )

        while (true) {
            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed >= MINIMUM_RUN_MS) break
            progress = paced(full, elapsed)
            delay(FRAME_MS)
        }
        progress = full
    }

    /**
     * Придерживает показ темпом «круг за [MINIMUM_RUN_MS]».
     *
     * Пара небольших файлов обрабатывается за доли секунды — быстрее, чем
     * кольцо успевает проехать хотя бы четверть круга. Экран в таком случае
     * мелькает, а анимации обрываются на полпути. Доли отдельных файлов
     * сжимаются тем же коэффициентом, чтобы очередь не разошлась с кольцом.
     */
    private fun paced(progress: ProcessingProgress, elapsed: Long): ProcessingProgress {
        val cap = (elapsed.toDouble() / MINIMUM_RUN_MS).coerceAtMost(1.0)
        if (progress.overall <= cap) return progress

        val scale = cap / progress.overall
        return ProcessingProgress(
            overall = cap,
            items = progress.items.map { ItemProgress(it.id, it.fraction * scale) },
            estimatedRemaining = progress.estimatedRemaining,
        )
    }

    /**
     * Отмена теперь именно отмена, а не уход с экрана: работу надо остановить
     * явно — сама она этого уже не сделает.
     */
    fun cancel() {
        analytics.didCancelRun((progress.overall * PERCENT).roundToInt())
        service.cancel()
        router.popToRoot()
    }

    // MARK: Показатели

    val percent: Int
        get() = (progress.overall * PERCENT).roundToInt()

    val runWord: String
        get() = copy.runWord(request.kind)

    /**
     * Пока прогноз не на чем строить, так и пишем: выдуманные «40 с»
     * на первых кадрах — обещание, которое некому сдержать.
     */
    val remainingText: String?
        get() = progress.estimatedRemaining?.let(formatters.durations::remaining)

    val countText: String
        get() = formatters.counts.string(items.size, CountNoun.FILE)

    fun fraction(item: MediaItem): Float = progress.fraction(item).toFloat()

    private companion object {
        const val MINIMUM_RUN_MS = 2000L
        const val FRAME_MS = 33L
        const val PERCENT = 100
    }
}
