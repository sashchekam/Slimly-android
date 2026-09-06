package com.slimly.navigation

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.slimly.designsystem.SlimlyColor
import com.slimly.designsystem.SlimlyRadius
import kotlin.coroutines.cancellation.CancellationException

/**
 * Рисует состояние роутера: стек — переходами внутри слоя, модалки — шторкой
 * и полноэкранным слоем. Внутри модалки поднимается такой же хост со своим
 * роутером, поэтому вложенность произвольная.
 *
 * Высоту шторке никто не задаёт: `ModalBottomSheet` берёт её у содержимого,
 * и иосный обход с подъёмом высоты предпочтением через `NavigationStack`
 * сюда не переносится.
 */
@Composable
fun RouterHost(router: Router, registry: RouteRegistry, modifier: Modifier = Modifier) {
    Box(modifier) {
        RouteStack(router = router, registry = registry)
        CoverHost(router = router, registry = registry)
    }
    SheetHost(router = router, registry = registry)
}

/**
 * Стек слоя: виден верхний экран, переход между экранами — тот же ход,
 * что у системного push, включая отставание уходящего.
 */
@Composable
private fun RouteStack(router: Router, registry: RouteRegistry) {
    val backProgress = remember { Animatable(0f) }
    var backEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    // Пока поверх слоя открыта модалка, системный «назад» принадлежит ей,
    // а не стеку под ней.
    PredictiveBackHandler(enabled = router.canPop && !router.hasModal) { events ->
        try {
            events.collect { event ->
                backEdge = event.swipeEdge
                backProgress.snapTo(event.progress)
            }
            router.pop()
            backProgress.snapTo(0f)
        } catch (ignored: CancellationException) {
            // Жест отменён на середине — экран возвращается на место.
            // Это сигнал самого жеста, а не сбой: гасим его здесь.
            backProgress.animateTo(0f)
        }
    }

    AnimatedContent(
        targetState = router.screens,
        transitionSpec = { stackTransform() },
        contentKey = { it.last() },
        label = "route-stack",
        modifier = Modifier.graphicsLayer {
            // Ответ на жест назад: экран отъезжает и уменьшается, показывая,
            // что отпускание уведёт его совсем. Так же ведёт себя система.
            val progress = backProgress.value
            val shrink = 1f - progress * BACK_SHRINK
            val direction = if (backEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
            scaleX = shrink
            scaleY = shrink
            translationX = progress * size.width * BACK_SHIFT * direction
        },
    ) { screens ->
        RouteScreen(route = screens.last(), router = router, registry = registry)
    }
}

/**
 * Ход перехода между экранами стека: приходящий приезжает от края,
 * уходящий отстаёт от него и уводится в противоположную сторону.
 *
 * Отставание обязательно: без него два экрана едут одной плитой, и глаз
 * не понимает, какой из них новый. Смена корня (стек той же глубины) идёт
 * как push — новый «мир» приходит, а не возвращается.
 */
private fun AnimatedContentTransitionScope<List<Route>>.stackTransform(): ContentTransform {
    val isForward = targetState.size >= initialState.size
    val spec = spring<IntOffset>(dampingRatio = SCREEN_DAMPING, stiffness = SCREEN_STIFFNESS)

    return ContentTransform(
        targetContentEnter = slideInHorizontally(spec) { width ->
            if (isForward) width else -(width * PARALLAX).toInt()
        },
        initialContentExit = slideOutHorizontally(spec) { width ->
            if (isForward) -(width * PARALLAX).toInt() else width
        },
        // Приходящий экран поверх уходящего на push и наоборот на pop:
        // ведущий край всегда у того, кто движется к зрителю.
        targetContentZIndex = targetState.size.toFloat(),
        sizeTransform = null,
    )
}

/** Шторка: свой роутер, свой стек, своя высота по содержимому. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetHost(router: Router, registry: RouteRegistry) {
    val route = router.sheet ?: return

    // Маршрут — это и есть личность слоя: без ключа Compose переиспользовал бы
    // состояние хоста, и шторка, открытая взамен закрывающейся, приехала бы
    // с чужим корнем.
    key(route) {
        ModalBottomSheet(
            onDismissRequest = router::dismissSheet,
            sheetState = rememberModalBottomSheetState(),
            containerColor = SlimlyColor.Sheet,
            contentColor = SlimlyColor.Text,
            shape = RoundedCornerShape(topStart = SlimlyRadius.Sheet, topEnd = SlimlyRadius.Sheet),
        ) {
            RouterHost(router = rememberRouter(route, router), registry = registry)
        }
    }
}

/** Слой поверх всего: камера, системный просмотрщик — то, у чего своя жизнь. */
@Composable
private fun CoverHost(router: Router, registry: RouteRegistry) {
    AnimatedContent(
        targetState = router.cover,
        transitionSpec = {
            slideInVertically { height -> height } togetherWith slideOutVertically { height -> height }
        },
        label = "route-cover",
    ) { route ->
        if (route != null) {
            key(route) {
                val coverRouter = rememberRouter(route, router)
                BackHandler(enabled = !coverRouter.canPop) { coverRouter.dismiss() }

                RouterHost(
                    router = coverRouter,
                    registry = registry,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlimlyColor.Background)
                        // Глухая подложка: экран под слоем не должен ловить
                        // нажатия сквозь него.
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                )
            }
        }
    }
}

/** Доля ширины, на которую уходящий экран отстаёт от приходящего. */
private const val PARALLAX = 0.28f

/** Ход экрана: длинный и без отскока — едет целый кадр, а не элемент в нём. */
private const val SCREEN_DAMPING = 0.95f
private const val SCREEN_STIFFNESS = 150f

/** На сколько экран уменьшается и отъезжает к концу жеста назад. */
private const val BACK_SHRINK = 0.08f
private const val BACK_SHIFT = 0.04f
