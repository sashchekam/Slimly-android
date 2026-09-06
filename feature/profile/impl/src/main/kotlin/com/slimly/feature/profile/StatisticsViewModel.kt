package com.slimly.feature.profile

import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.ProcessingDigest
import com.slimly.core.model.ProcessingHistory
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.StatsRange
import com.slimly.core.model.ToolStats
import com.slimly.core.service.MediaProcessingService
import com.slimly.navigation.Router

/**
 * Подробный разрез сделанного: срез времени, доли инструментов и карточка
 * на каждый из них.
 */
class StatisticsViewModel(
    private val resources: Resources,
    private val service: MediaProcessingService,
    private val formatters: SlimlyFormatters,
    private val analytics: ProfileAnalytics,
    private val router: Router,
) {
    var range: StatsRange by mutableStateOf(StatsRange.QUARTER)
        private set

    var history: ProcessingHistory by mutableStateOf(ProcessingHistory.EMPTY)
        private set

    /**
     * Срез считается один раз на смену данных или периода, а не на каждое
     * чтение: экран трогает его из десятка мест, а под ним лежит журнал
     * на тысячу записей.
     */
    var digest: ProcessingDigest by mutableStateOf(ProcessingHistory.EMPTY.digest(StatsRange.QUARTER))
        private set

    suspend fun load() {
        history = service.history()
        digest = history.digest(range)
    }

    // MARK: Срез

    val ranges: List<StatsRange> = StatsRange.entries

    fun select(range: StatsRange) {
        if (range == this.range) return
        this.range = range
        digest = history.digest(range)
        analytics.didChangeRange(range)
    }

    fun title(range: StatsRange): String = resources.getString(range.titleRes)

    val periodText: String
        get() = formatters.dates.periodText(digest)

    val isEmpty: Boolean
        get() = digest.isEmpty

    /**
     * Что показать вместо цифр, пока их нет. Разные срезы пусты по разным
     * причинам, и предлагать «сделайте что-нибудь» тому, кто просто выбрал
     * прошлый месяц, не за что.
     */
    val emptyText: String
        get() = resources.getString(
            if (history.isEmpty) R.string.statistics_empty else R.string.statistics_empty_period,
        )

    // MARK: Операции

    val totalText: String
        get() = formatters.counts.string(digest.total)

    val shares: List<ToolShare>
        get() = digest.tools.map { tool ->
            val fraction = digest.share(tool.kind)
            ToolShare(
                kind = tool.kind,
                label = resources.getString(tool.kind.shortTitleRes),
                fraction = fraction,
                percentText = formatters.items.percent(fraction),
            )
        }

    // MARK: Карточки инструментов

    val cards: List<ToolCard>
        get() {
            val peak = digest.tools.maxOfOrNull { it.count } ?: 0

            return digest.tools.map { tool ->
                ToolCard(
                    kind = tool.kind,
                    title = resources.getString(tool.kind.titleRes),
                    countText = formatters.counts.string(tool.count),
                    unit = formatters.counts.word(tool.count, tool.kind.unit),
                    note = note(tool),
                    spark = tool.spark,
                    facts = facts(tool),
                    isLeading = tool.count > 0 && tool.count == peak,
                )
            }
        }

    private fun note(tool: ToolStats): String = if (tool.isEmpty) emptyNote(tool.kind) else filledNote(tool)

    private fun filledNote(tool: ToolStats): String = when (tool.kind) {
        ProcessingKind.COMPRESSION -> resources.getString(
            R.string.summary_compression,
            formatters.sizes.string(tool.savedByteCount),
            formatters.items.percent(tool.reduction),
        )

        ProcessingKind.CONVERSION -> conversionNote(tool)

        ProcessingKind.AUDIO_EXTRACTION -> resources.getString(
            R.string.summary_audio,
            tool.formats.firstOrNull() ?: DEFAULT_AUDIO_FORMAT,
            formatters.durations.total(tool.seconds),
        )

        ProcessingKind.SCAN -> resources.getString(
            R.string.summary_scan,
            formatters.counts.string(tool.pageCount, CountNoun.PAGE),
            formatters.counts.string(tool.pagesPerDocument, CountNoun.PAGE),
        )
    }

    private fun conversionNote(tool: ToolStats): String {
        val top = tool.formatPairs.firstOrNull() ?: return resources.getString(R.string.summary_conversion_unknown)
        val second = tool.formatPairs.getOrNull(1)
            ?: return resources.getString(R.string.summary_conversion_single, top)
        return resources.getString(R.string.summary_conversion_pair, top, second)
    }

    private fun emptyNote(kind: ProcessingKind): String = resources.getString(
        when (kind) {
            ProcessingKind.COMPRESSION -> R.string.empty_compression
            ProcessingKind.CONVERSION -> R.string.empty_conversion
            ProcessingKind.AUDIO_EXTRACTION -> R.string.empty_audio
            ProcessingKind.SCAN -> R.string.empty_scan
        },
    )

    private fun facts(tool: ToolStats): List<ToolFact> = when (tool.kind) {
        ProcessingKind.COMPRESSION -> listOf(
            fact(formatters.sizes.string(tool.savedByteCount), R.string.fact_freed),
            fact(formatters.items.percent(tool.reduction), R.string.fact_average_gain),
        )

        ProcessingKind.CONVERSION -> listOf(
            fact(tool.formatPairs.firstOrNull(), R.string.fact_common_pair),
            fact(mixText(tool), R.string.fact_mix),
        )

        ProcessingKind.AUDIO_EXTRACTION -> listOf(
            fact(formatters.durations.total(tool.seconds), R.string.fact_duration),
            fact(tool.formats.firstOrNull(), R.string.fact_main_format),
        )

        ProcessingKind.SCAN -> listOf(
            fact(formatters.counts.string(tool.pageCount, CountNoun.PAGE), R.string.fact_captured),
            fact(tool.formats.firstOrNull() ?: DEFAULT_SCAN_FORMAT, R.string.fact_format),
        )
    }

    /**
     * «26 фото · 5 видео». Пустая половина выпадает: «0 видео» ничего
     * не уточняет, а место занимает.
     */
    private fun mixText(tool: ToolStats): String = buildList {
        if (tool.photoCount > 0) add(formatters.counts.string(tool.photoCount, CountNoun.PHOTO))
        if (tool.videoCount > 0) add(formatters.counts.string(tool.videoCount, CountNoun.VIDEO))
    }.joinToString(separator = " · ")

    /**
     * Прочерк вместо пустого значения: клетка без содержимого читается как
     * сбой вёрстки, а прочерк — как честное «нечего показать».
     */
    private fun fact(value: String?, labelRes: Int): ToolFact = ToolFact(
        value = if (value.isNullOrEmpty()) DASH else value,
        label = resources.getString(labelRes),
    )

    // MARK: Навигация

    fun goBack() {
        router.pop()
    }

    private companion object {
        const val DASH = "—"
        const val DEFAULT_AUDIO_FORMAT = "M4A"
        const val DEFAULT_SCAN_FORMAT = "PDF"
    }
}

/** Доля инструмента: кусок полосы и строка легенды под ней. */
data class ToolShare(val kind: ProcessingKind, val label: String, val fraction: Double, val percentText: String)

/** Пара «значение — подпись» в подвале карточки инструмента. */
data class ToolFact(val value: String, val label: String)

/** Карточка инструмента целиком. */
data class ToolCard(
    val kind: ProcessingKind,
    val title: String,
    val countText: String,
    val unit: String,
    /**
     * Одна фраза о том, что стоит за числом. Не пересказ цифры: цифра уже
     * написана рядом крупно.
     */
    val note: String,
    val spark: List<Double>,
    val facts: List<ToolFact>,
    /** Ведущий инструмент среза — он один красится акцентом. */
    val isLeading: Boolean,
)
