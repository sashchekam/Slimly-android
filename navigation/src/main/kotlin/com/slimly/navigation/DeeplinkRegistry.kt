package com.slimly.navigation

import com.slimly.core.deeplink.DeeplinkUrl

/**
 * Куда ведёт ссылка: стек маршрутов и, если нужно, модалка поверх него.
 *
 * Модалка нужна там, где экран и с хаба открывается шторкой: ссылка
 * не должна показывать его иначе, чем кнопка.
 */
data class DeeplinkDestination(
    /** Первый маршрут становится корнем слоя, остальные ложатся в стек. */
    val routes: List<Route>,
    /** Что показать шторкой поверх стека. */
    val sheet: Route? = null,
) {
    constructor(vararg routes: Route) : this(routes.toList())

    val isEmpty: Boolean
        get() = routes.isEmpty() && sheet == null
}

/**
 * Разбирает одну ссылку. `null` — ссылка не его, пусть пробует следующий.
 */
fun interface DeeplinkMapper {
    fun destination(deeplink: DeeplinkUrl): DeeplinkDestination?
}

/**
 * Превращает разобранную ссылку в состояние навигации.
 *
 * Первый мэппер, вернувший непустой результат, выигрывает. Сами шаблоны
 * адресов живут в `:app`: только там известны маршруты всех фич сразу.
 */
class DeeplinkRegistry(private val mappers: List<DeeplinkMapper>) {
    fun destination(deeplink: DeeplinkUrl): DeeplinkDestination? = mappers.firstNotNullOfOrNull { mapper ->
        mapper.destination(deeplink)?.takeUnless { it.isEmpty }
    }
}

/**
 * Ссылка — это одно присвоение состояния: стек раскладывается в слой,
 * модалка встаёт поверх него.
 */
fun Router.navigate(destination: DeeplinkDestination) {
    replace(destination.routes)
    destination.sheet?.let(::present)
}
