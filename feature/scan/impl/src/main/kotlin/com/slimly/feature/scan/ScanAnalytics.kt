package com.slimly.feature.scan

import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsValue
import com.slimly.core.analytics.FeatureAnalytics

/**
 * Аналитика скана.
 *
 * Единственный инструмент со своим началом: у скана есть выбор источника
 * и съёмка страниц, которых нет ни у кого больше. Дальше начинается общий
 * флоу и его события `flow_*`.
 */
class ScanAnalytics(override val analytics: AnalyticsService) : FeatureAnalytics {
    override val scope: String = "scan"

    /**
     * Откуда берут страницы. Камера и галерея — разные сценарии: документ
     * снимают на месте, а из галереи собирают то, что сняли раньше.
     */
    fun didPickSource(source: ScanSource) {
        track("source_picked", mapOf("source" to AnalyticsValue.of(source.value)))
    }

    /**
     * Съёмка закончена, страницы уходят дальше. Режим — тот, что стоял
     * на последнем кадре: по нему видно, доверяют ли автоспуску.
     */
    fun didCapturePages(count: Int, isAutomatic: Boolean) {
        track(
            "pages_captured",
            mapOf(
                "count" to AnalyticsValue.of(count),
                "mode" to AnalyticsValue.of(if (isAutomatic) "auto" else "manual"),
            ),
        )
    }

    /**
     * Из камеры вышли, снятое выброшено. Число страниц отделяет
     * передумавшего на первом кадре от того, у кого съёмка не задалась.
     */
    fun didCancelCamera(count: Int) {
        track("camera_cancelled", mapOf("count" to AnalyticsValue.of(count)))
    }

    /**
     * Границы страниц приняты. `edited` и `missed` — оценка автоматики
     * человеком: сколько рамок пришлось поправить и на скольких страницах
     * лист вообще не нашёлся.
     */
    fun didConfirmBounds(count: Int, edited: Int, missed: Int) {
        track(
            "bounds_confirmed",
            mapOf(
                "count" to AnalyticsValue.of(count),
                "edited" to AnalyticsValue.of(edited),
                "missed" to AnalyticsValue.of(missed),
            ),
        )
    }
}

/** Откуда берутся страницы документа. */
enum class ScanSource(val value: String) {
    CAMERA("camera"),
    GALLERY("gallery"),
}
