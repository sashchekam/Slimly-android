package com.slimly.firebase

import android.content.Context
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.NoAnalytics
import com.slimly.core.remoteconfig.LocalRemoteConfig
import com.slimly.core.remoteconfig.RemoteConfigService
import com.slimly.firebase.analytics.FirebaseAnalyticsService
import com.slimly.firebase.remoteconfig.FirebaseRemoteConfigService

/**
 * Сервисы, собранные поверх Firebase, — единственное, что модуль отдаёт
 * наружу. Сами типы SDK за границу не выходят: приложение получает
 * [AnalyticsService] и [RemoteConfigService] и про Firebase не знает ничего.
 *
 * Без `google-services.json` состав подменяется тихими заглушками: сборка
 * без ключей — это рабочая сборка без аналитики, а не упавшая.
 */
class FirebaseServices(val analytics: AnalyticsService, val remoteConfig: RemoteConfigService) {
    companion object {
        /** Состав без SDK: события уходят в никуда, конфиг отдаёт умолчания. */
        fun disabled(): FirebaseServices = FirebaseServices(NoAnalytics(), LocalRemoteConfig())

        /**
         * Состав по месту. [isDebug] решает не состав, а поведение конфига:
         * отладочной сборке кэш не нужен.
         */
        fun of(context: Context, isDebug: Boolean): FirebaseServices = of(context, isDebug, FirebaseBootstrap())

        internal fun of(context: Context, isDebug: Boolean, bootstrap: FirebaseBootstrapping): FirebaseServices =
            if (bootstrap.start(context)) {
                FirebaseServices(
                    analytics = FirebaseAnalyticsService(context),
                    remoteConfig = FirebaseRemoteConfigService(isDebug),
                )
            } else {
                disabled()
            }
    }
}
