package com.slimly.feature.home

import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsValue
import com.slimly.core.analytics.FeatureAnalytics
import com.slimly.feature.home.api.HomeTool

/**
 * Аналитика хаба: чем вообще пользуются.
 *
 * Дальше путь инструмента пишет общий флоу — здесь только точка входа
 * в него, но именно по ней видно, какая карточка живая, а какая просто
 * занимает место.
 */
class HomeAnalytics(override val analytics: AnalyticsService) : FeatureAnalytics {
    override val scope: String = "home"

    /** Инструмент открыт с главной. */
    fun didOpenTool(tool: HomeTool) {
        track("tool_opened", mapOf("tool" to AnalyticsValue.of(tool.id)))
    }

    /** Открыт профиль — экран статистики и настроек. */
    fun didOpenProfile() {
        track("profile_opened")
    }
}
