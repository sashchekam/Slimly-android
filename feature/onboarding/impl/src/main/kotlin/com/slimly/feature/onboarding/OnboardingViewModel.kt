package com.slimly.feature.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.service.OnboardingState
import com.slimly.core.service.markTourSeen
import com.slimly.navigation.Route
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val state: OnboardingState,
    /**
     * Куда уходит тур на последнем шаге. Приходит готовым маршрутом: какая
     * фича стоит за главным экраном, туру знать незачем.
     */
    private val home: Route,
    private val analytics: OnboardingAnalytics,
    private val router: Router,
    private val scope: CoroutineScope,
    val pages: List<OnboardingPage> = OnboardingPage.TOUR,
) {
    var pageIndex: Int by mutableStateOf(0)
        private set

    val currentPage: OnboardingPage
        get() = pages[pageIndex]

    val isLastPage: Boolean
        get() = pageIndex == pages.lastIndex

    fun next() {
        if (isLastPage) {
            finish(skipped = false)
            return
        }
        pageIndex += 1
        analytics.didOpenPage(pageIndex)
    }

    /**
     * «Пропустить» и крестик — одно и то же действие: тур засчитан
     * показанным. Второй раз его никто не ждёт, а если понадобится —
     * он открывается ссылкой.
     */
    fun skip() {
        finish(skipped = true)
    }

    private fun finish(skipped: Boolean) {
        analytics.didFinish(pageIndex, skipped)
        // Отметка уходит в хранилище, а корень меняется сразу: ждать записи
        // на диск ради перехода на главный экран незачем.
        scope.launch { state.markTourSeen() }
        router.restart(home)
    }
}
