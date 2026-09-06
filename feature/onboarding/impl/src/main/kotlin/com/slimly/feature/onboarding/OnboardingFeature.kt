package com.slimly.feature.onboarding

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.service.OnboardingState
import com.slimly.feature.onboarding.api.OnboardingRoute
import com.slimly.navigation.Route
import com.slimly.navigation.RouteResolver
import com.slimly.navigation.routeResolver

/**
 * Точка сборки тура.
 *
 * `home` приходит готовым маршрутом, а не типом: тур заканчивается сменой
 * корня, но какая фича стоит за главным экраном — не его дело.
 */
fun onboardingResolver(state: OnboardingState, home: Route, analytics: AnalyticsService): RouteResolver =
    routeResolver<OnboardingRoute> { route, router ->
        val scope = rememberCoroutineScope()

        when (route) {
            OnboardingRoute.Tour -> OnboardingScreen(
                viewModel = remember(router) {
                    OnboardingViewModel(
                        state = state,
                        home = home,
                        analytics = OnboardingAnalytics(analytics),
                        router = router,
                        scope = scope,
                    )
                },
            )
        }
    }
