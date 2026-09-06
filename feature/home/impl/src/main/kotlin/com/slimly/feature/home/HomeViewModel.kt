package com.slimly.feature.home

import com.slimly.feature.home.api.HomeTool
import com.slimly.navigation.Route
import com.slimly.navigation.Router
import com.slimly.navigation.show
import java.time.LocalTime

class HomeViewModel(
    /**
     * Список собирается на каждом обращении, а не хранится готовым: подписи
     * плиток переведены, а язык меняют на ходу — снимок остался бы
     * на прежнем.
     */
    private val makeTools: () -> List<HomeTool>,
    private val profile: Route,
    private val analytics: HomeAnalytics,
    private val router: Router,
) {
    val tools: List<HomeTool>
        get() = makeTools()

    /**
     * Макет показывает дневной вариант, но одна и та же строка круглые сутки
     * читалась бы как баг — время берём в момент показа.
     */
    val greeting: Greeting
        get() = when (LocalTime.now().hour) {
            in MORNING until AFTERNOON -> Greeting.MORNING
            in AFTERNOON until EVENING -> Greeting.AFTERNOON
            in EVENING until NIGHT -> Greeting.EVENING
            else -> Greeting.NIGHT
        }

    fun open(tool: HomeTool) {
        analytics.didOpenTool(tool)
        router.show(tool.route, tool.presentation)
    }

    fun openProfile() {
        analytics.didOpenProfile()
        router.push(profile)
    }

    private companion object {
        const val MORNING = 5
        const val AFTERNOON = 12
        const val EVENING = 18
        const val NIGHT = 23
    }
}

enum class Greeting {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT,
}
