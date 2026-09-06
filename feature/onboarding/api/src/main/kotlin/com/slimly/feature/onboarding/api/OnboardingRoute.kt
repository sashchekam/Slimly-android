package com.slimly.feature.onboarding.api

import com.slimly.navigation.Route
import kotlinx.parcelize.Parcelize

/**
 * Публичный вход в вводный тур.
 *
 * Тур — это корень слоя, а не экран поверх приложения: он показывается вместо
 * главного и заканчивается сменой корня. Возврата из него нет, поэтому
 * и маршрута «назад» здесь не заведено.
 */
@Parcelize
enum class OnboardingRoute : Route {
    Tour,
}
