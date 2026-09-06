package com.slimly.navigation

import kotlinx.parcelize.Parcelize

/** Маршруты для тестов: роутеру всё равно, что за экраны за ними стоят. */
@Parcelize
internal enum class Screen : Route {
    Home,
    Details,
    Settings,
    Preview,
}
