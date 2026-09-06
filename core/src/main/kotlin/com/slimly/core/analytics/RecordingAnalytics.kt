package com.slimly.core.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Приёмник, который никуда не отправляет: он складывает отправленное
 * в [AnalyticsLog], откуда это показывает отладочное меню.
 *
 * В отладочной сборке стоит рядом с настоящей аналитикой, в
 * [CompositeAnalytics]: событие уходит наружу и одновременно остаётся
 * на устройстве, где его видно сразу, а не через сутки в веб-консоли.
 */
class RecordingAnalytics(
    private val log: AnalyticsLog,
    /** Отправляют аналитику откуда угодно, а журнал живёт на главном потоке. */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate),
) : AnalyticsService {
    override fun track(event: AnalyticsEvent) {
        append(AnalyticsRecord(name = event.name, content = AnalyticsRecord.Content.Event(event.parameters)))
    }

    override fun set(property: AnalyticsUserProperty, value: String?) {
        append(AnalyticsRecord(name = property.name, content = AnalyticsRecord.Content.Property(value)))
    }

    private fun append(record: AnalyticsRecord) {
        scope.launch { log.append(record) }
    }
}
