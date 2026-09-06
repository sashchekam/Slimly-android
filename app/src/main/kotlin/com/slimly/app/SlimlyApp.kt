package com.slimly.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slimly.designsystem.SlimlyScreenBackground
import com.slimly.feature.debug.DebugStatusChip
import com.slimly.feature.debug.api.DebugRoute
import com.slimly.navigation.Route
import com.slimly.navigation.Router
import com.slimly.navigation.RouterHost
import com.slimly.navigation.rememberRouter

/**
 * Корень интерфейса: заставка, а под ней роутер со своим хостом.
 *
 * Роутер создаётся здесь, а не в контейнере: стек навигации переживает смерть
 * процесса и приезжает обратно из `savedInstanceState`, а это умеет только
 * состояние композиции.
 */
@Composable
fun SlimlyApp(container: AppContainer, modifier: Modifier = Modifier) {
    // Заставка отыгрывается один раз за жизнь Activity: после поворота знак
    // не должен обходить круг заново.
    var isSplashDone by rememberSaveable { mutableStateOf(false) }

    // С чего начинать — известно только после чтения отметки о туре. Ждать
    // её незачем: пока идёт заставка, ответ уже приходит.
    var start by rememberSaveable { mutableStateOf<Route?>(null) }
    LaunchedEffect(Unit) { start = container.startRoute() }

    Box(modifier.fillMaxSize()) {
        // Подложка не участвует в передаче кадра: пока заставка гаснет,
        // а первый экран ещё не проявился, между ними виден фон приложения,
        // а не чёрное окно.
        SlimlyScreenBackground()

        val root = start
        if (root != null) {
            // Заставка сначала полностью уходит и только потом проявляется
            // первый экран: одновременный кроссфейд смешал бы два кадра
            // в мутную середину.
            AnimatedVisibility(
                visible = isSplashDone,
                enter = fadeIn(tween(durationMillis = APPEAR_MILLIS, delayMillis = APPEAR_DELAY_MILLIS)),
                exit = fadeOut(),
            ) {
                AppContent(container, root)
            }
        }

        AnimatedVisibility(visible = !isSplashDone, enter = fadeIn(), exit = fadeOut(tween(FADE_MILLIS))) {
            SplashScreen(onFinish = { isSplashDone = true })
        }
    }
}

@Composable
private fun AppContent(container: AppContainer, root: Route) {
    val router = rememberRouter(root)

    // Ссылка ждёт в ящике, пока экран не появится: до первого кадра открывать
    // нечего, а при холодном старте она приходит раньше него.
    LaunchedEffect(router) {
        container.pendingDeeplinks.collect { deeplink -> container.open(deeplink, router) }
    }

    Box(Modifier.fillMaxSize()) {
        RouterHost(router = router, registry = container.registry, modifier = Modifier.fillMaxSize())
        DebugBadge(container, router)
    }
}

/**
 * Метка DEBUG поверх всего: она горит, пока в отладочном меню включена хоть
 * одна подмена, и по нажатию открывает это меню.
 *
 * Живёт над роутером, а не в шапке экрана: подмена действует везде. Под уже
 * открытым слоем поверх всего — камерой или самим меню — метка гаснет: она
 * напоминает о подмене, а не мешает тому, что показано.
 */
@Composable
private fun BoxScope.DebugBadge(container: AppContainer, router: Router) {
    val settings = container.debugSettings ?: return
    if (!settings.isOverridden || router.cover != null) return

    DebugStatusChip(
        onClick = { router.cover(DebugRoute.Menu) },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(BADGE_INSET),
    )
}

private val BADGE_INSET = 12.dp
private const val FADE_MILLIS = 300
private const val APPEAR_MILLIS = 350
private const val APPEAR_DELAY_MILLIS = 260
