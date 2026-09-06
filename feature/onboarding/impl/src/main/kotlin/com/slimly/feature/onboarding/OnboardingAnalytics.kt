package com.slimly.feature.onboarding

import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsValue
import com.slimly.core.analytics.FeatureAnalytics

/**
 * Аналитика вводного тура.
 *
 * Тур — это первое, что видит человек, и единственное место, где отвал стоит
 * дорого: ушедший отсюда не увидел ни одного инструмента. Поэтому событий
 * два — переход по страницам и то, чем тур закончился.
 */
class OnboardingAnalytics(override val analytics: AnalyticsService) : FeatureAnalytics {
    override val scope: String = "onboarding"

    /**
     * Открыта следующая страница тура. Номер человеческий, с единицы:
     * в отчёте он читается как «докрутили до третьей».
     */
    fun didOpenPage(index: Int) {
        track("page_opened", mapOf("page" to AnalyticsValue.of(index + 1)))
    }

    /**
     * Тур закончен. `skipped` отделяет дочитавших от нажавших «Пропустить»:
     * экран приложения оба увидят, но пришли к нему по-разному.
     */
    fun didFinish(index: Int, skipped: Boolean) {
        track(
            "finished",
            mapOf("page" to AnalyticsValue.of(index + 1), "skipped" to AnalyticsValue.of(skipped)),
        )
    }
}
