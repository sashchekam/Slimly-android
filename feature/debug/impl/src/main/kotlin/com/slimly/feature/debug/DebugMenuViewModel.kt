package com.slimly.feature.debug

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.analytics.AnalyticsLog
import com.slimly.core.debug.DebugSettings
import com.slimly.core.debug.SimulatedFailure
import com.slimly.core.service.AppEnvironment
import com.slimly.core.service.AppEnvironmentProviding
import com.slimly.core.service.OnboardingState
import com.slimly.core.service.ProcessingStatsStore
import com.slimly.feature.debug.api.DebugRoute
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Группа взаимоисключающих подмен: одна крутилка поведения. */
data class DebugKnob(val id: DebugKnobId, val title: String, val hint: String, val options: List<DebugOption>)

enum class DebugKnobId {
    FAILURE,
    ONBOARDING,
}

data class DebugOption(val id: String, val title: String, val isSelected: Boolean)

/** Раздел меню — отдельный экран, а не переключатель в списке. */
enum class DebugSection(val route: DebugRoute) {
    REMOTE_CONFIG(DebugRoute.RemoteConfig),
    ANALYTICS(DebugRoute.Analytics),
}

/** Разовое действие: сработало — и всё, состояния у него нет. */
enum class DebugAction(@get:StringRes val titleRes: Int, @get:StringRes val hintRes: Int) {
    COPY_DIAGNOSTICS(R.string.action_copy_diagnostics, R.string.action_copy_diagnostics_hint),
    RESET_STATS(R.string.action_reset_stats, R.string.action_reset_stats_hint),
    RESET_KNOBS(R.string.action_reset_knobs, R.string.action_reset_knobs_hint),
}

/**
 * Сервисный экран, а не часть продукта: здесь подменяют поведение сборки
 * и забирают данные о ней.
 *
 * Хранилище статистики и отметка о туре приходят теми же экземплярами, что
 * и у обработчика со стартом: сброс из меню должен обнулять те цифры,
 * которые показывает профиль, а не свою копию.
 */
class DebugMenuViewModel(
    private val settings: DebugSettings,
    private val statsStore: ProcessingStatsStore,
    private val onboarding: OnboardingState,
    private val analyticsLog: AnalyticsLog,
    private val environmentProvider: AppEnvironmentProviding,
    private val clipboard: DebugClipboard,
    private val resources: Resources,
    private val router: Router,
    private val scope: CoroutineScope,
) {
    var toast: String? by mutableStateOf(null)
        private set

    /**
     * Копия отметки о туре: хранилище об изменениях не сообщает, а чип
     * выбранного варианта должен перекрашиваться сразу.
     */
    private var isTourSeen: Boolean by mutableStateOf(false)

    private var environment: AppEnvironment? by mutableStateOf(null)

    private var toastJob: Job? = null

    suspend fun load() {
        isTourSeen = onboarding.hasSeenTour()
        environment = environmentProvider.environment()
    }

    /** Шапка меню: с неё начинается любое обращение в поддержку. */
    val summary: String
        get() = environment?.summary.orEmpty()

    // MARK: Крутилки

    val knobs: List<DebugKnob>
        get() = listOf(failureKnob, tourKnob)

    private val failureKnob: DebugKnob
        get() = DebugKnob(
            id = DebugKnobId.FAILURE,
            title = resources.getString(R.string.knob_failure_title),
            hint = resources.getString(R.string.knob_failure_note),
            options = SimulatedFailure.entries.map { failure ->
                DebugOption(
                    id = failure.name,
                    title = resources.getString(failure.titleRes),
                    isSelected = failure == settings.simulatedFailure,
                )
            },
        )

    /**
     * Тур — корень первого запуска, а не окно поверх хаба: пересобрать корень
     * на живом экране значит выкинуть человека из текущего флоу, поэтому меню
     * только переставляет отметку, а увидят её при следующем запуске.
     */
    private val tourKnob: DebugKnob
        get() = DebugKnob(
            id = DebugKnobId.ONBOARDING,
            title = resources.getString(R.string.knob_tour_title),
            hint = resources.getString(R.string.knob_tour_note),
            options = listOf(
                DebugOption(SEEN, resources.getString(R.string.knob_seen), isTourSeen),
                DebugOption(UNSEEN, resources.getString(R.string.knob_unseen), !isTourSeen),
            ),
        )

    fun select(option: DebugOption, knob: DebugKnob) {
        when (knob.id) {
            DebugKnobId.FAILURE -> {
                val failure = SimulatedFailure.entries.firstOrNull { it.name == option.id } ?: return
                scope.launch { settings.setSimulatedFailure(failure) }
            }

            DebugKnobId.ONBOARDING -> {
                val seen = option.id == SEEN
                isTourSeen = seen
                scope.launch { onboarding.setTourSeen(seen) }
                say(resources.getString(if (seen) R.string.knob_tour_marked else R.string.knob_tour_cleared))
            }
        }
    }

    // MARK: Разделы

    val sections: List<DebugSection> = DebugSection.entries

    fun title(section: DebugSection): String = when (section) {
        // Имя продукта, а не наше: в веб-консоли раздел называется так же,
        // и переводить его значило бы прятать то, что ищут глазами.
        DebugSection.REMOTE_CONFIG -> REMOTE_CONFIG_TITLE

        DebugSection.ANALYTICS -> resources.getString(R.string.menu_analytics)
    }

    /**
     * Подпись раздела считается на месте, а не лежит в самом разделе: она
     * говорит о текущем состоянии, и, вернувшись с экрана, видно, осталось ли
     * что-то подменённым.
     */
    fun value(section: DebugSection): String = when (section) {
        DebugSection.REMOTE_CONFIG -> {
            val count = settings.remoteConfigOverrides.names.size
            if (count == 0) {
                resources.getString(R.string.config_summary)
            } else {
                resources.getString(R.string.config_summary_overrides, count)
            }
        }

        DebugSection.ANALYTICS -> {
            val count = analyticsLog.records.size
            if (count == 0) {
                resources.getString(R.string.analytics_summary_empty)
            } else {
                resources.getString(R.string.analytics_summary_records, count)
            }
        }
    }

    fun open(section: DebugSection) {
        router.push(section.route)
    }

    // MARK: Действия

    val actions: List<DebugAction> = DebugAction.entries

    fun perform(action: DebugAction) {
        when (action) {
            DebugAction.COPY_DIAGNOSTICS -> {
                clipboard.copy(summary)
                say(resources.getString(R.string.action_copy_diagnostics_done))
            }

            DebugAction.RESET_STATS -> scope.launch {
                statsStore.reset()
                say(resources.getString(R.string.action_reset_stats_done))
            }

            DebugAction.RESET_KNOBS -> scope.launch {
                settings.reset()
                say(resources.getString(R.string.action_reset_knobs_done))
            }
        }
    }

    /** Меню приезжает поверх всего — «Готово» его и закрывает. */
    fun close() {
        router.dismiss()
    }

    private fun say(text: String) {
        toastJob?.cancel()
        toast = text
        toastJob = scope.launch {
            delay(TOAST_MILLIS)
            toast = null
        }
    }

    private companion object {
        const val SEEN = "seen"
        const val UNSEEN = "unseen"
        const val REMOTE_CONFIG_TITLE = "Remote Config"
        const val TOAST_MILLIS = 2600L
    }
}
