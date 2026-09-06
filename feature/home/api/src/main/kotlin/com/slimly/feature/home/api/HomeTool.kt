package com.slimly.feature.home.api

import com.slimly.navigation.Route
import com.slimly.navigation.RoutePresentation

/**
 * Карточка инструмента на хабе.
 *
 * Маршрут приходит значением, поэтому главная не зависит ни от одной чужой
 * фичи: список собирает `:app` — единственное место, где видны маршруты всех
 * инструментов сразу.
 */
data class HomeTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val route: Route,
    /**
     * Способ показа выбирает не карточка, а тот, кто собирает список: скан
     * открывается шторкой выбора источника, остальные — экраном.
     */
    val presentation: RoutePresentation = RoutePresentation.PUSH,
)
