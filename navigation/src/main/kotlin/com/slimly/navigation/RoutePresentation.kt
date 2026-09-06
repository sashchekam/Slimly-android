package com.slimly.navigation

/**
 * Способ показа маршрута.
 *
 * Маршрут о нём не знает: один и тот же экран можно и запушить, и поднять
 * шторкой, и показать поверх всего — выбирает тот, кто открывает.
 */
enum class RoutePresentation {
    PUSH,
    SHEET,

    /**
     * Поверх всего экрана — для того, у чего своя полноэкранная жизнь:
     * камера, системный просмотрщик документа.
     */
    COVER,
}

fun Router.show(route: Route, presentation: RoutePresentation) {
    when (presentation) {
        RoutePresentation.PUSH -> push(route)
        RoutePresentation.SHEET -> present(route)
        RoutePresentation.COVER -> cover(route)
    }
}
