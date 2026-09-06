package com.slimly.feature.compress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.CompressionLevel
import com.slimly.core.model.CompressionPreset
import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingRequest
import com.slimly.feature.compress.api.CompressRoute
import com.slimly.navigation.Router

/**
 * Единственное, что есть у сжатия своего: одна настройка, ползунком.
 *
 * Остальные шаги — общие и живут в `:mediaflow`.
 */
class LevelSheetViewModel(
    val items: List<MediaItem>,
    private val formatters: SlimlyFormatters,
    private val analytics: CompressAnalytics,
    private val router: Router,
) {
    var level: CompressionLevel by mutableStateOf(CompressionLevel.DEFAULT)
        private set

    val presets: List<CompressionPreset> = CompressionPreset.entries

    fun isNearest(preset: CompressionPreset): Boolean = level.nearestPreset == preset

    fun setLevel(value: Float) {
        level = CompressionLevel.of(value.toDouble())
    }

    /**
     * Нажатие на подпись пресета ставит ползунок на его засечку — подпись
     * работает как кнопка, но шкала остаётся непрерывной.
     */
    fun select(preset: CompressionPreset) {
        level = CompressionLevel.at(preset)
    }

    // MARK: Показатели

    private val request: ProcessingRequest
        get() = ProcessingRequest(items = items, operation = ProcessingOperation.Compress(level))

    val deltaText: String
        get() = "−${request.deltaPercent}%"

    val beforeText: String
        get() = formatters.sizes.string(request.originalByteCount)

    val afterText: String
        get() = formatters.sizes.string(request.estimatedByteCount)

    val remainingFraction: Float
        get() = level.ratio.toFloat().coerceAtMost(1f)

    val selectionText: String
        get() = "${formatters.counts.string(items.size, CountNoun.FILE)}, $beforeText"

    // MARK: Навигация

    fun cancel() {
        router.dismiss()
    }

    /**
     * Обработка идёт в стеке под шторкой: шторка закрывается, а экран
     * обработки встаёт поверх выбора файлов, а не внутри модалки.
     */
    fun start() {
        val request = request
        analytics.didPickLevel(level, request)

        val parent = router.parent ?: router
        router.dismiss()
        parent.push(CompressRoute.Run(request))
    }
}
