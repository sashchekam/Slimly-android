package com.slimly.feature.profile.api

import com.slimly.navigation.Route
import kotlinx.parcelize.Parcelize

/** Маршруты профиля. См. комментарий у `HomeRoute`. */
@Parcelize
enum class ProfileRoute : Route {
    Profile,

    /** Подробная статистика — отдельный экран поверх профиля. */
    Statistics,
}
