package com.slimly.feature.home

import androidx.compose.runtime.remember
import com.slimly.core.analytics.AnalyticsService
import com.slimly.feature.home.api.HomeRoute
import com.slimly.feature.home.api.HomeTool
import com.slimly.navigation.Route
import com.slimly.navigation.RouteResolver
import com.slimly.navigation.routeResolver

/**
 * Точка сборки фичи: единственное, что `:app` о ней знает.
 *
 * Список инструментов и маршрут профиля приходят параметрами — главная умеет
 * их открыть, но видит от чужих фич только значения маршрутов.
 */
fun homeResolver(tools: () -> List<HomeTool>, profile: Route, analytics: AnalyticsService): RouteResolver =
    routeResolver<HomeRoute> { _, router ->
        HomeScreen(
            viewModel = remember(router) {
                HomeViewModel(
                    makeTools = tools,
                    profile = profile,
                    analytics = HomeAnalytics(analytics),
                    router = router,
                )
            },
        )
    }
