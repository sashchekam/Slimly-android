package com.slimly.feature.compress

import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsValue
import com.slimly.core.analytics.FeatureAnalytics
import com.slimly.core.model.CompressionLevel
import com.slimly.core.model.ProcessingRequest

/**
 * Аналитика сжатия.
 *
 * Шаги выбора, обработки и итога у сжатия общие с остальными инструментами
 * и пишутся в `flow_*`. Своё здесь одно — степень сжатия: это единственная
 * настройка, которую человек крутит руками, и по ней видно, чего от приложения
 * ждут — незаметной копии или самого лёгкого файла.
 */
class CompressAnalytics(override val analytics: AnalyticsService) : FeatureAnalytics {
    override val scope: String = "compress"

    /**
     * Степень выбрана, сжатие запущено. Пресет — ближайшая засечка шкалы:
     * ползунок непрерывный, и точное число в отчёте не с чем сравнивать.
     */
    fun didPickLevel(level: CompressionLevel, request: ProcessingRequest) {
        track(
            "level_picked",
            mapOf(
                "preset" to AnalyticsValue.of(level.nearestPreset.name.lowercase()),
                "delta_percent" to AnalyticsValue.of(request.deltaPercent),
            ),
        )
    }
}
