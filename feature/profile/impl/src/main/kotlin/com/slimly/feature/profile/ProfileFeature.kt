package com.slimly.feature.profile

import android.content.res.Resources
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.service.AppEnvironmentProviding
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaProcessingService
import com.slimly.feature.profile.api.ProfileRoute
import com.slimly.navigation.Route
import com.slimly.navigation.RouteResolver
import com.slimly.navigation.routeResolver

/**
 * Точка сборки фичи: единственное, что `:app` о ней знает.
 *
 * `debugMenu` приходит готовым маршрутом, а не типом: профиль не должен
 * знать, что за модуль стоит за отладочным меню и есть ли он в сборке.
 */
fun profileResolver(
    resources: Resources,
    library: MediaLibrary,
    service: MediaProcessingService,
    environment: AppEnvironmentProviding,
    formatters: SlimlyFormatters,
    analytics: AnalyticsService,
    debugMenu: Route? = null,
): RouteResolver {
    val profileAnalytics = ProfileAnalytics(analytics)

    return routeResolver<ProfileRoute> { route, router ->
        val scope = rememberCoroutineScope()

        when (route) {
            ProfileRoute.Profile -> ProfileScreen(
                viewModel = remember(router) {
                    ProfileViewModel(
                        resources = resources,
                        library = library,
                        service = service,
                        environmentProvider = environment,
                        formatters = formatters,
                        analytics = profileAnalytics,
                        router = router,
                        scope = scope,
                        debugMenu = debugMenu,
                    )
                },
            )

            ProfileRoute.Statistics -> StatisticsScreen(
                viewModel = remember(router) {
                    StatisticsViewModel(
                        resources = resources,
                        service = service,
                        formatters = formatters,
                        analytics = profileAnalytics,
                        router = router,
                    )
                },
            )
        }
    }
}
