package com.slimly.feature.debug

import android.content.res.Resources
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.slimly.core.analytics.AnalyticsLog
import com.slimly.core.debug.DebugSettings
import com.slimly.core.remoteconfig.RemoteConfigService
import com.slimly.core.service.AppEnvironmentProviding
import com.slimly.core.service.OnboardingState
import com.slimly.core.service.ProcessingStatsStore
import com.slimly.feature.debug.api.DebugRoute
import com.slimly.navigation.RouteResolver
import com.slimly.navigation.routeResolver

/**
 * Точка сборки отладочного меню.
 *
 * Собирается только в отладочной сборке: в релизе резолвер не регистрируется
 * вовсе, и маршрут `DebugRoute` некому разобрать — как и некому его положить.
 *
 * Хранилище статистики и отметка о туре приходят теми же экземплярами, что
 * и у обработчика со стартом: сброс из меню должен обнулять те цифры, которые
 * показывает профиль, а не свою копию.
 *
 * @param remoteConfig источник значений **без** подмен поверх: экран конфига
 *   показывает, что реально приехало с сервера, и подмену рядом.
 * @param analyticsLog тот же журнал, в который пишет аналитика сборки: меню
 *   показывает отправленное, а не свою копию.
 */
fun debugResolver(
    resources: Resources,
    settings: DebugSettings,
    statsStore: ProcessingStatsStore,
    onboarding: OnboardingState,
    remoteConfig: RemoteConfigService,
    analyticsLog: AnalyticsLog,
    environment: AppEnvironmentProviding,
    clipboard: DebugClipboard,
): RouteResolver = routeResolver<DebugRoute> { route, router ->
    val scope = rememberCoroutineScope()

    when (route) {
        DebugRoute.Menu -> DebugMenuScreen(
            viewModel = remember(router) {
                DebugMenuViewModel(
                    settings = settings,
                    statsStore = statsStore,
                    onboarding = onboarding,
                    analyticsLog = analyticsLog,
                    environmentProvider = environment,
                    clipboard = clipboard,
                    resources = resources,
                    router = router,
                    scope = scope,
                )
            },
        )

        DebugRoute.RemoteConfig -> DebugRemoteConfigScreen(
            viewModel = remember(router) {
                DebugRemoteConfigViewModel(
                    settings = settings,
                    remoteConfig = remoteConfig,
                    resources = resources,
                    router = router,
                    scope = scope,
                )
            },
        )

        DebugRoute.Analytics -> DebugAnalyticsScreen(
            viewModel = remember(router) {
                DebugAnalyticsViewModel(
                    log = analyticsLog,
                    clipboard = clipboard,
                    resources = resources,
                    router = router,
                    scope = scope,
                )
            },
        )
    }
}
