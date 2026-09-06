package com.slimly.mediaflow

import com.slimly.navigation.Route
import com.slimly.navigation.Router

/**
 * Куда ведёт шаг общего флоу.
 *
 * Маршрут не знает про способ показа: настройки сжатия приезжают шторкой,
 * а настройки конвертации — экраном в стеке. Решает это фича, которая
 * собирает шаг, а сам шаг просто исполняет её решение.
 */
sealed interface FlowDestination {
    val route: Route

    data class Push(override val route: Route) : FlowDestination

    data class Present(override val route: Route) : FlowDestination

    /**
     * Поверх всего экрана — для того, у чего своя полноэкранная жизнь:
     * камера, системный просмотрщик документа.
     */
    data class Cover(override val route: Route) : FlowDestination
}

/**
 * Исполнить решение фичи о способе показа.
 *
 * Разбор [FlowDestination] одинаков у всех шагов, и держать его копию
 * в каждой вьюмодели значило бы трижды описывать одно и то же.
 */
fun Router.go(destination: FlowDestination) {
    when (destination) {
        is FlowDestination.Push -> push(destination.route)
        is FlowDestination.Present -> present(destination.route)
        is FlowDestination.Cover -> cover(destination.route)
    }
}
