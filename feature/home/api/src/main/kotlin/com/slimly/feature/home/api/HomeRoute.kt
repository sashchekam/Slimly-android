package com.slimly.feature.home.api

import com.slimly.navigation.Route
import kotlinx.parcelize.Parcelize

/**
 * Маршруты главной. Снаружи фича видна только этим типом — реализацию
 * не импортирует никто, кроме composition root.
 */
@Parcelize
enum class HomeRoute : Route {
    Home,
}
