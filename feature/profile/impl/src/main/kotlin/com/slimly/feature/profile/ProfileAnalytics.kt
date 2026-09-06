package com.slimly.feature.profile

import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsValue
import com.slimly.core.analytics.FeatureAnalytics
import com.slimly.core.model.StatsRange

/**
 * Аналитика профиля.
 *
 * Профиль — единственный экран без работы: сюда приходят посмотреть на себя.
 * Отсюда и вопросы к нему: доходят ли вообще до подробной статистики
 * и какой срез времени человеку интересен.
 */
class ProfileAnalytics(override val analytics: AnalyticsService) : FeatureAnalytics {
    override val scope: String = "profile"

    /**
     * Открыт разбор по инструментам. Источник важнее самого факта: плитка
     * в карточке и ссылка под ней ведут в одно место, но плитку нажимают
     * с вопросом про конкретный инструмент.
     */
    fun didOpenStatistics(source: StatisticsSource) {
        track("statistics_opened", mapOf("source" to AnalyticsValue.of(source.code)))
    }

    /**
     * Сменили срез времени. Умолчание — 90 дней; если его почти всегда
     * меняют, значит выбрано неверно.
     */
    fun didChangeRange(range: StatsRange) {
        track("statistics_range_changed", mapOf("range" to AnalyticsValue.of(code(range))))
    }

    /**
     * Ушли выбирать язык в системные настройки.
     *
     * Интересна не сама смена, а то, каким языкам не хватает перевода: назад
     * система нам о выборе не сообщает, но сам уход показывает, что наш
     * перевод человека не устроил.
     */
    fun didOpenLanguageSettings() {
        track("language_settings_opened")
    }

    private fun code(range: StatsRange): String = when (range) {
        StatsRange.MONTH -> "days_30"
        StatsRange.QUARTER -> "days_90"
        StatsRange.ALL_TIME -> "all_time"
    }
}

/** Откуда пришли в подробную статистику. */
enum class StatisticsSource(val code: String) {
    /** Плитка инструмента в карточке «чем вы пользовались». */
    TILE("tile"),

    /** Ссылка «Вся статистика» под плитками. */
    LINK("link"),
}
