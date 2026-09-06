package com.slimly.mediaflow

import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsUserProperty
import com.slimly.core.analytics.AnalyticsValue
import com.slimly.core.analytics.FeatureAnalytics
import com.slimly.core.model.MediaItem
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingTarget
import com.slimly.core.model.originalByteCount
import com.slimly.core.model.outputByteCount
import com.slimly.core.model.totalByteCount
import com.slimly.core.service.MediaAccess
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * Аналитика общего пути обработки: доступ → выбор файлов → запуск → итог →
 * сохранение.
 *
 * Живёт рядом с шагами, а не в фичах: экраны у инструментов общие, и своя
 * копия тех же событий в каждой фиче означала бы четыре описания одной
 * воронки. Инструмент едет параметром `tool` — по нему отчёт режется,
 * а воронка остаётся одна.
 *
 * Своё, чего у соседа нет, каждый инструмент пишет сам: степень сжатия —
 * в `CompressAnalytics`.
 */
class FlowAnalytics(override val analytics: AnalyticsService, private val tool: FlowTool) : FeatureAnalytics {
    override val scope: String = "flow"

    /**
     * Система ответила на запрос доступа к медиатеке — первая развилка любого
     * инструмента: отказ обрывает флоу на самом входе, и без этого события
     * такой уход выглядел бы как обычный отвал с экрана выбора.
     */
    fun didAnswerAccess(access: MediaAccess) {
        track("access_answered", mapOf("tool" to toolValue, "status" to AnalyticsValue.of(code(access))))
    }

    /** Файлы выбраны, инструмент пошёл к настройкам. */
    fun didPickFiles(items: List<MediaItem>) {
        track(
            "files_picked",
            mapOf(
                "tool" to toolValue,
                "count" to AnalyticsValue.of(items.size),
                "input_mb" to megabytes(items.totalByteCount),
            ),
        )
    }

    /** Обработка началась — настройки приняты, дальше работает устройство. */
    fun didStartRun(request: ProcessingRequest) {
        track(
            "run_started",
            mapOf(
                "tool" to toolValue,
                "count" to AnalyticsValue.of(request.items.size),
                "format" to AnalyticsValue.of(code(request.targetFormat?.target)),
                "input_mb" to megabytes(request.originalByteCount),
            ),
        )
    }

    /**
     * Обработка дошла до конца. Вес «до» и «после» отправляем парой: по ним
     * видно и выигрыш сжатия, и то, что конвертация иногда утяжеляет файл.
     */
    fun didFinishRun(outcome: ProcessingOutcome, duration: Duration) {
        track(
            "run_finished",
            mapOf(
                "tool" to toolValue,
                "count" to AnalyticsValue.of(outcome.results.size),
                "failed" to AnalyticsValue.of(outcome.failures.size),
                "duration_ms" to AnalyticsValue.of(duration.inWholeMilliseconds.toInt()),
                "input_mb" to megabytes(outcome.results.originalByteCount),
                "output_mb" to megabytes(outcome.results.outputByteCount),
            ),
        )
    }

    /**
     * С экрана обработки ушли, не дождавшись конца: уход рвёт поток, и готовых
     * файлов после него не остаётся. Процент говорит, терпения не хватило
     * или что-то встало.
     */
    fun didCancelRun(percent: Int) {
        track("run_cancelled", mapOf("tool" to toolValue, "percent" to AnalyticsValue.of(percent)))
    }

    /**
     * Готовые файлы сохранены — ради этого затевался весь флоу.
     *
     * Здесь же ставится свойство «дошёл до файла»: делить аудиторию
     * на попробовавших и оставшихся на первом экране нужно ровно по этому
     * моменту, и разносить его с событием было бы негде забыть.
     */
    fun didSaveResults(outcome: ProcessingOutcome) {
        analytics.set(hasSavedResult, "true")
        track(
            "result_saved",
            mapOf(
                "tool" to toolValue,
                "count" to AnalyticsValue.of(outcome.results.size),
                "destination" to AnalyticsValue.of(code(outcome.results.firstOrNull()?.destination)),
                "output_mb" to megabytes(outcome.results.outputByteCount),
            ),
        )
    }

    /**
     * Часть файлов сохранить не удалось. Отдельным событием, а не полем
     * в успехе: обработка прошла, а до диска файлы не дошли — это разные
     * поломки и чинятся они в разных местах.
     */
    fun didFailSaving(count: Int) {
        track("save_failed", mapOf("tool" to toolValue, "count" to AnalyticsValue.of(count)))
    }

    /** Открыли просмотр готового файла — результату не поверили на слово. */
    fun didPreviewResult() {
        track("result_previewed", mapOf("tool" to toolValue))
    }

    /**
     * Готовому файлу поменяли имя. Событие редкое по замыслу: если имя правят
     * часто, значит, автоматическое имя людям не подходит.
     */
    fun didRenameResult() {
        track("result_renamed", mapOf("tool" to toolValue))
    }

    /** Оригиналы удалены после сохранения: место освобождено по-настоящему. */
    fun didDeleteOriginals(count: Int) {
        track("originals_deleted", mapOf("tool" to toolValue, "count" to AnalyticsValue.of(count)))
    }

    /**
     * Доступ к медиатеке как срез: с частичным доступом человек видит
     * не свою галерею, а отобранное однажды, и его поведение сравнивать
     * с остальными нельзя.
     */
    fun setAccess(access: MediaAccess) {
        analytics.set(mediaAccess, code(access))
    }

    private val toolValue: AnalyticsValue
        get() = AnalyticsValue.of(tool.value)

    /**
     * Формат результата коротким кодом, а не подписью из интерфейса:
     * в подписи живут точки-разделители и переведённые слова, а отчёт
     * режется по значению целиком.
     */
    private fun code(target: ProcessingTarget?): String = when (target) {
        // У сжатия формат не меняется — цели нет, и писать в отчёт нечего.
        null -> "original"

        is ProcessingTarget.Image -> target.format.name.lowercase()

        is ProcessingTarget.Video -> "${target.container.name}_${target.codec.name}".lowercase()

        is ProcessingTarget.Audio -> target.format.containerName.lowercase()

        is ProcessingTarget.Document -> "pdf_${target.mode.name.lowercase()}"
    }

    private fun code(access: MediaAccess): String = access.name.lowercase()

    private fun code(destination: OutputDestination?): String = (destination ?: OutputDestination.GALLERY)
        .name
        .lowercase()

    /**
     * Байты в мегабайты с одним знаком: в отчёте нужны сравнимые величины,
     * а точное число байт всё равно никто не читает.
     */
    private fun megabytes(bytes: Long): AnalyticsValue =
        AnalyticsValue.of((bytes.toDouble() / BYTES_IN_MEGABYTE * TENTHS).roundToInt() / TENTHS)

    private companion object {
        const val BYTES_IN_MEGABYTE = 1_048_576.0
        const val TENTHS = 10.0
    }
}

/** Доступ к медиатеке: `authorized`, `limited`, `denied`. */
val mediaAccess = AnalyticsUserProperty("media_access")

/** Человек хоть раз сохранил готовый файл. */
val hasSavedResult = AnalyticsUserProperty("has_saved_result")
