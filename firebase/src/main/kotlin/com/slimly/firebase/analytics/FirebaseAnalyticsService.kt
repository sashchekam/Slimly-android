package com.slimly.firebase.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.slimly.core.analytics.AnalyticsEvent
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsUserProperty

/**
 * Общая аналитика приложения на Firebase.
 *
 * Всё, что она делает, — чинит имена и раскладывает параметры: очередь,
 * пакетную отправку и повторы держит SDK.
 */
class FirebaseAnalyticsService internal constructor(
    private val firebase: FirebaseAnalytics,
    private val names: AnalyticsNameNormalizing = AnalyticsNameNormalizer(),
    private val parameters: AnalyticsParameterEncoding = AnalyticsParameterEncoder(),
) : AnalyticsService {
    constructor(context: Context) : this(FirebaseAnalytics.getInstance(context))

    override fun track(event: AnalyticsEvent) {
        firebase.logEvent(names.normalized(event.name), parameters.encoded(event.parameters))
    }

    override fun set(property: AnalyticsUserProperty, value: String?) {
        firebase.setUserProperty(names.normalized(property.name), value)
    }
}
