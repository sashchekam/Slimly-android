package com.slimly.core.model

import java.time.Instant
import kotlin.math.roundToInt

private const val MILLIS_IN_DAY = 86_400_000L

/**
 * Что сделал один инструмент за выбранный срез.
 *
 * Числа сырые, без подписей: одно и то же количество страниц экран
 * показывает и цифрой, и внутри фразы, и склонять их дважды по-разному
 * не стоит — этим занимается тот, кто рисует.
 */
data class ToolStats(
    val kind: ProcessingKind,
    /** Операций: файлов, а у скана — собранных документов. */
    val count: Int,
    val originalByteCount: Long,
    val outputByteCount: Long,
    val photoCount: Int,
    val videoCount: Int,
    val pageCount: Int,
    /** Суммарная длительность исходников — у извлечения звука. */
    val seconds: Double,
    /** Пары «из → в» по убыванию частоты: «HEIC → JPEG». */
    val formatPairs: List<String>,
    /** Форматы результата по убыванию частоты: «M4A», «PDF». */
    val formats: List<String>,
    /** Высоты столбиков искры, 0…1. Длина — [ProcessingDigest.SPARK_BAR_COUNT]. */
    val spark: List<Double>,
) {
    val isEmpty: Boolean
        get() = count == 0

    val savedByteCount: Long
        get() = originalByteCount - outputByteCount

    /**
     * Средний выигрыш по весу: 0.58 — это «−58%». Считается по общему весу,
     * а не как среднее из процентов: иначе один крошечный файл весил бы
     * в среднем столько же, сколько часовое видео.
     */
    val reduction: Double
        get() = if (originalByteCount > 0) 1 - outputByteCount.toDouble() / originalByteCount else 0.0

    /**
     * Страниц на документ — округляем вверх: «в среднем 3,5 страницы»
     * звучит точнее, чем есть.
     */
    val pagesPerDocument: Int
        get() = if (count > 0) (pageCount.toDouble() / count).roundToInt() else 0

    internal companion object {
        fun of(kind: ProcessingKind, records: List<ProcessingRecord>, interval: DateInterval?): ToolStats = ToolStats(
            kind = kind,
            count = records.sumOf { it.count },
            originalByteCount = records.sumOf { it.originalByteCount },
            outputByteCount = records.sumOf { it.outputByteCount },
            photoCount = records.sumOf { it.photoCount },
            videoCount = records.sumOf { it.videoCount },
            pageCount = records.sumOf { it.pageCount },
            seconds = records.sumOf { it.seconds },
            formatPairs = ranked(records.map { it.formatPair }),
            formats = ranked(records.map { it.targetFormat }),
            spark = sparkBars(records, interval),
        )

        /**
         * Значения по убыванию частоты. Одинаково частые идут по алфавиту:
         * «чаще всего» не должно меняться местами от запуска к запуску.
         */
        private fun ranked(values: List<String>): List<String> = values
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }

        /**
         * Столбики искры: количество операций по равным долям отрезка,
         * поделённое на самый высокий столбик.
         */
        private fun sparkBars(records: List<ProcessingRecord>, interval: DateInterval?): List<Double> {
            val bars = ProcessingDigest.SPARK_BAR_COUNT
            val buckets = MutableList(bars) { 0.0 }
            if (interval == null || interval.durationMillis <= 0) return buckets

            records.forEach { record ->
                val elapsed = record.date.toEpochMilli() - interval.start.toEpochMilli()
                val offset = elapsed.toDouble() / interval.durationMillis
                val index = (offset * bars).toInt().coerceIn(0, bars - 1)
                buckets[index] += record.count
            }

            val peak = buckets.max()
            return if (peak > 0) buckets.map { it / peak } else buckets
        }
    }
}

/** Статистика за срез: сколько работ и как они разошлись по инструментам. */
data class ProcessingDigest(
    val range: StatsRange,
    /**
     * Отрезок, за который посчитано. `null` — у «всего времени», пока не
     * сделано ни одной обработки: подписывать нечего.
     */
    val interval: DateInterval?,
    /**
     * Все виды работ в порядке [ProcessingKind.entries], включая пустые:
     * «ни разу» — тоже ответ, и ноль честнее пропущенной строки.
     */
    val tools: List<ToolStats>,
) {
    val total: Int
        get() = tools.sumOf { it.count }

    val isEmpty: Boolean
        get() = total == 0

    /** Доля инструмента в общем числе операций — ширина его куска полосы. */
    fun share(kind: ProcessingKind): Double = if (total > 0) stats(kind).count.toDouble() / total else 0.0

    fun stats(kind: ProcessingKind): ToolStats = // Список собран из `entries`, поэтому промах здесь означал бы,
        // что вид работы завели мимо перечисления.
        tools.firstOrNull { it.kind == kind } ?: ToolStats.of(kind, emptyList(), null)

    companion object {
        /**
         * Двенадцать столбиков искры: меньше — рисунок распадается на
         * отдельные палки, больше — не читается на ширине карточки.
         */
        const val SPARK_BAR_COUNT = 12

        fun of(records: List<ProcessingRecord>, range: StatsRange, now: Instant): ProcessingDigest {
            // У среза с глубиной начало отсчитывается назад от «сейчас»,
            // у «всего времени» — берётся из первой записи журнала.
            val start = range.days
                ?.let { now.minusMillis(it * MILLIS_IN_DAY) }
                ?: records.firstOrNull()?.date
            val interval = start?.let { DateInterval(start = minOf(it, now), end = now) }
            val slice = interval?.let { window -> records.filter { it.date in window } }.orEmpty()

            return ProcessingDigest(
                range = range,
                interval = interval,
                tools = ProcessingKind.entries.map { kind ->
                    ToolStats.of(kind, slice.filter { it.kind == kind }, interval)
                },
            )
        }
    }
}
