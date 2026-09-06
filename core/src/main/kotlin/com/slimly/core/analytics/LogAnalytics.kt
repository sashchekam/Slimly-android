package com.slimly.core.analytics

import android.util.Log

/**
 * Событие в системный лог — то же имя и те же параметры, что уйдут наружу.
 *
 * Тег отдельный: по нему в logcat отфильтровывается вся аналитика сборки,
 * не перемешиваясь с остальными сообщениями приложения.
 */
class LogAnalytics : AnalyticsService {
    override fun track(event: AnalyticsEvent) {
        Log.d(TAG, "→ ${event.name} ${described(event.parameters)}")
    }

    override fun set(property: AnalyticsUserProperty, value: String?) {
        Log.d(TAG, "· ${property.name} = ${value ?: "—"}")
    }

    /**
     * Параметры печатаются по алфавиту: одинаковые события должны выглядеть
     * в логе одинаково, иначе их не сравнить глазами.
     */
    private fun described(parameters: Map<String, AnalyticsValue>): String {
        if (parameters.isEmpty()) return ""

        return parameters.entries
            .sortedBy { it.key }
            .joinToString(prefix = "[", postfix = "]") { "${it.key}=${it.value.text}" }
    }

    private companion object {
        const val TAG = "slimly.analytics"
    }
}
