package com.slimly.mediakit.processing

import com.slimly.core.model.ProcessingEvent
import com.slimly.core.model.ProcessingProgress
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingRun
import com.slimly.mediakit.processing.foreground.ForegroundWorkHolding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/** Идущая работа: одна на приложение. */
internal interface ProcessingRunning {
    val active: StateFlow<ProcessingRun?>

    fun events(request: ProcessingRequest): Flow<ProcessingEvent>

    fun cancel()
}

/**
 * Работа живёт в приложении, а не в подписке на её события.
 *
 * Здесь главное отличие от iOS: там уход с экрана обрывал поток, а вместе
 * с ним и работу. На Android свернуть приложение посреди обработки — обычное
 * дело, поэтому очередь крутится в собственной области видимости, а поток
 * событий её только показывает. Последнее событие лежит в буфере: экран,
 * вернувшийся после поворота, подхватывает прогресс, а не начинает заново.
 */
internal class ProcessingRunner(
    private val queue: ProcessingQueueing,
    private val foreground: ForegroundWorkHolding,
    dispatcher: CoroutineDispatcher,
) : ProcessingRunning {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val current = MutableStateFlow<ProcessingRun?>(null)

    override val active: StateFlow<ProcessingRun?> = current.asStateFlow()

    /**
     * Последнее событие, а не их очередь: прогресс идёт чаще, чем экран
     * успевает рисовать, и показывать надо свежую долю, а не все подряд.
     * `null` — работы ещё не было или её сменили новой.
     */
    private val stream = MutableStateFlow<ProcessingEvent?>(null)

    private var job: Job? = null
    private var running: ProcessingRequest? = null

    /** Отличает свою уборку от чужой, когда одно задание сменило другое. */
    private var generation = 0

    override fun events(request: ProcessingRequest): Flow<ProcessingEvent> {
        if (running != request) start(request)
        return stream.filterNotNull()
    }

    override fun cancel() {
        job?.cancel()
    }

    private fun start(request: ProcessingRequest) {
        if (request.items.isEmpty()) return

        job?.cancel()
        stream.value = null
        running = request
        current.value = ProcessingRun(request, ProcessingProgress.idle(request.items))
        foreground.begin()

        val mine = ++generation
        job = scope.launch {
            try {
                val tracker = ProgressTracker(request.items) { progress ->
                    current.value = ProcessingRun(request, progress)
                    stream.value = ProcessingEvent.Progress(progress)
                }
                stream.value = ProcessingEvent.Finished(queue.run(request, tracker))
            } finally {
                // Своё задание уже сменили другим — тогда и уведомление,
                // и сервис принадлежат уже не нам.
                if (generation == mine) {
                    current.value = null
                    foreground.end()
                }
            }
        }
    }
}
