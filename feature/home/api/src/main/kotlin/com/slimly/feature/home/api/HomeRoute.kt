package com.slimly.feature.home.api

/**
 * Маршруты главной. Снаружи фича видна только этим типом — реализацию
 * не импортирует никто, кроме composition root.
 *
 * Общий тип `Route` и роутер приезжают в фазе 1 вместе с `:navigation`.
 */
enum class HomeRoute {
    Home,
}
