package com.slimly.navigation

import androidx.compose.runtime.Composable

/**
 * Знает, как построить экран по маршрутам одного типа.
 *
 * Резолвер объявляет сама фича — она единственная знает свои экраны
 * и вьюмодели, — а зависимости в него прокидывает `:app` при сборке реестра.
 * Собирается резолвер через [routeResolver], где тип маршрута выводится
 * из вызова.
 */
class RouteResolver internal constructor(
    internal val canHandle: (Route) -> Boolean,
    internal val content: @Composable (Route, Router) -> Unit,
) {
    companion object {
        /**
         * Резолвер маршрутов одного типа. Приведение живёт здесь, а не
         * на месте вызова, и оно безопасно: тип уже проверен `canHandle`,
         * а до содержимого маршрут иначе не доходит.
         */
        @Suppress("UNCHECKED_CAST")
        fun <R : Route> forType(type: Class<R>, content: @Composable (R, Router) -> Unit): RouteResolver =
            RouteResolver(
                canHandle = { type.isInstance(it) },
                content = { route, router -> content(route as R, router) },
            )
    }
}

inline fun <reified R : Route> routeResolver(noinline content: @Composable (R, Router) -> Unit): RouteResolver =
    RouteResolver.forType(R::class.java, content)

/**
 * Реестр «маршрут → экран». Собирается в `:app` — это единственное место,
 * где известны все фичи сразу.
 */
class RouteRegistry(private val resolvers: List<RouteResolver>) {
    internal fun resolver(route: Route): RouteResolver? = resolvers.firstOrNull { it.canHandle(route) }
}

/**
 * Экран маршрута. Маршрут, для которого забыли зарегистрировать резолвер,
 * показывается заглушкой: видимый экран с текстом лучше пустоты или падения
 * в проде.
 */
@Composable
internal fun RouteScreen(route: Route, router: Router, registry: RouteRegistry) {
    val resolver = registry.resolver(route)
    if (resolver == null) {
        UnresolvedRouteScreen(route)
    } else {
        resolver.content(route, router)
    }
}
