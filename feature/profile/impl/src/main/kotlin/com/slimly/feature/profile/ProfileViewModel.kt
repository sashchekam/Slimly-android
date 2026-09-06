package com.slimly.feature.profile

import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.FileSizeParts
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.LibrarySnapshot
import com.slimly.core.model.ProcessingDigest
import com.slimly.core.model.ProcessingHistory
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.ProcessingStats
import com.slimly.core.model.StatsRange
import com.slimly.core.service.AppEnvironment
import com.slimly.core.service.AppEnvironmentProviding
import com.slimly.core.service.MediaAccess
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaProcessingService
import com.slimly.feature.profile.api.ProfileRoute
import com.slimly.navigation.Route
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Профиль: медиатека, сделанное нами и чем именно пользовались.
 *
 * Цифры свои, а не системные: медиатека о наших сжатиях ничего не знает,
 * и всё, что здесь показано, приложение записало о себе само.
 */
class ProfileViewModel(
    private val resources: Resources,
    private val library: MediaLibrary,
    private val service: MediaProcessingService,
    private val environmentProvider: AppEnvironmentProviding,
    private val formatters: SlimlyFormatters,
    private val analytics: ProfileAnalytics,
    private val router: Router,
    private val scope: CoroutineScope,
    /**
     * Маршрут отладочного меню. В релизной сборке его нет — и строка версии
     * остаётся обычным показанием, а не спрятанной дверью.
     */
    private val debugMenu: Route? = null,
    /**
     * Системный выбор языка. Публичный: сам переход показывает Activity —
     * сервис на уровне приложения системный экран открыть не может.
     */
    val languageSettings: LanguageSettings = LanguageSettings(),
) {
    var snapshot: LibrarySnapshot? by mutableStateOf(null)
        private set

    var stats: ProcessingStats by mutableStateOf(ProcessingStats.EMPTY)
        private set

    /**
     * Срез журнала для карточки «чем вы пользовались». Считается один раз
     * при загрузке: под ним лежит журнал на тысячу записей, а экран читает
     * плитки на каждой перерисовке.
     */
    var usageDigest: ProcessingDigest by mutableStateOf(ProcessingHistory.EMPTY.digest(USAGE_RANGE))
        private set

    var access: MediaAccess by mutableStateOf(MediaAccess.NOT_DETERMINED)
        private set

    var environment: AppEnvironment? by mutableStateOf(null)
        private set

    var toast: String? by mutableStateOf(null)
        private set

    private var versionTaps = 0
    private var toastJob: Job? = null

    /**
     * Доступ здесь только показываем: спрашивать его в профиле не за чем —
     * системный запрос уместен там, где выбирают файлы.
     */
    suspend fun load() {
        coroutineScope {
            val snapshotTask = async { library.snapshot() }
            val statsTask = async { service.stats() }
            val historyTask = async { service.history() }
            val environmentTask = async { environmentProvider.environment() }

            access = library.access()
            snapshot = snapshotTask.await()
            stats = statsTask.await()
            usageDigest = historyTask.await().digest(USAGE_RANGE)
            environment = environmentTask.await()
        }
    }

    // MARK: Медиатека

    val photoCountText: String
        get() = formatters.counts.string(snapshot?.photoCount ?: 0)

    val videoCountText: String
        get() = formatters.counts.string(snapshot?.videoCount ?: 0)

    /**
     * Подписи под цифрами медиатеки. Слово согласуется с числом: формы уже
     * лежат в каталоге, и фиксированное «photos», как на iOS, здесь было бы
     * шагом назад.
     */
    val photoCaption: String
        get() = formatters.counts.word(snapshot?.photoCount ?: 0, CountNoun.PHOTO)

    val videoCaption: String
        get() = formatters.counts.word(snapshot?.videoCount ?: 0, CountNoun.VIDEO)

    val freeSpace: FileSizeParts
        get() = formatters.sizes.parts(snapshot?.freeSpace ?: 0)

    // MARK: Сжато нами

    val savedText: String
        get() = formatters.sizes.string(stats.savedByteCount)

    val remainingFraction: Float
        get() = stats.remainingFraction.toFloat()

    val compressedText: String
        get() = resources.getString(R.string.profile_now, formatters.sizes.string(stats.compressedByteCount))

    val originalText: String
        get() = resources.getString(R.string.profile_was, formatters.sizes.string(stats.originalByteCount))

    /**
     * Три пары «значение — подпись» под полосой: фото, видео, конвертации.
     *
     * Слово согласуется с числом, в отличие от iOS: там подпись фиксирована
     * («photos» при любом количестве), а здесь формы уже лежат в каталоге —
     * не пользоваться ими ради совпадения с чужой платформой незачем.
     */
    val breakdown: List<ProfileFact>
        get() = listOf(
            ProfileFact(
                value = formatters.counts.string(stats.photoCount),
                caption = formatters.counts.word(stats.photoCount, CountNoun.PHOTO),
            ),
            ProfileFact(
                value = formatters.counts.string(stats.videoCount),
                caption = formatters.counts.word(stats.videoCount, CountNoun.VIDEO),
            ),
            ProfileFact(
                value = formatters.counts.string(stats.conversionCount),
                caption = formatters.counts.word(stats.conversionCount, CountNoun.CONVERSION),
            ),
        )

    // MARK: Чем пользовались

    val usageCaption: String
        get() = resources.getString(R.string.profile_over, resources.getString(USAGE_RANGE.titleRes))

    val usage: List<UsageTile>
        get() {
            val peak = usageDigest.tools.maxOfOrNull { it.count } ?: 0

            return usageDigest.tools.map { tool ->
                UsageTile(
                    kind = tool.kind,
                    title = resources.getString(tool.kind.titleRes),
                    countText = formatters.counts.string(tool.count),
                    unit = formatters.counts.word(tool.count, tool.kind.unit),
                    share = if (peak > 0) tool.count.toDouble() / peak else 0.0,
                    isLeading = tool.count > 0 && tool.count == peak,
                )
            }
        }

    // MARK: Настройки

    val settingsRows: List<SettingsRow>
        get() = listOfNotNull(
            languageRow,
            SettingsRow(
                id = SettingsRowId.ACCESS,
                label = resources.getString(R.string.profile_gallery_access),
                value = accessText,
                isTappable = false,
            ),
            SettingsRow(
                id = SettingsRowId.VERSION,
                label = resources.getString(R.string.profile_version),
                value = environment?.versionText.orEmpty(),
                isTappable = debugMenu != null,
            ),
        )

    /**
     * Строки языка нет там, где её некуда вести: системный выбор языка
     * приложения появился в Android 13, а до него приложение говорит на языке
     * телефона и выбирать не из чего.
     */
    private val languageRow: SettingsRow?
        get() = if (!languageSettings.isAvailable) {
            null
        } else {
            SettingsRow(
                id = SettingsRowId.LANGUAGE,
                label = resources.getString(R.string.language_title),
                value = languageSettings.currentName(resources),
                isTappable = true,
            )
        }

    private val accessText: String
        get() = resources.getString(
            when (access) {
                MediaAccess.AUTHORIZED -> R.string.permission_authorized
                MediaAccess.LIMITED -> R.string.permission_limited
                MediaAccess.DENIED -> R.string.permission_denied
                MediaAccess.NOT_DETERMINED -> R.string.permission_not_determined
            },
        )

    // MARK: Навигация

    fun goBack() {
        router.pop()
    }

    /**
     * Плитка и ссылка ведут на один экран: плитка — не фильтр, а тот же
     * вопрос, заданный с конкретного инструмента в руках.
     */
    fun openStatistics(source: StatisticsSource) {
        analytics.didOpenStatistics(source)
        router.push(ProfileRoute.Statistics)
    }

    /** Экран ушёл в системные настройки языка — записываем сам факт ухода. */
    fun languageSettingsOpened() {
        analytics.didOpenLanguageSettings()
    }

    /**
     * Пять нажатий на версию открывают отладочное меню.
     *
     * Второй вход рядом с тряской нужен потому, что трясти телефон в чужих
     * руках — так себе инструкция: по телефону проще сказать «нажмите
     * пять раз на версию».
     */
    fun tapVersion() {
        val debugMenu = debugMenu ?: return

        versionTaps += 1
        when {
            versionTaps >= TAPS_TO_DEBUG -> {
                versionTaps = 0
                router.cover(debugMenu)
            }

            versionTaps >= TAPS_TO_HINT ->
                say(resources.getString(R.string.profile_debug_countdown, TAPS_TO_DEBUG - versionTaps))
        }
    }

    private fun say(text: String) {
        toastJob?.cancel()
        toast = text
        toastJob = scope.launch {
            delay(TOAST_MILLIS)
            toast = null
        }
    }

    companion object {
        /**
         * Срез для карточки в профиле фиксирован: она отвечает на «чем я
         * пользуюсь сейчас», а выбор глубины живёт на экране статистики,
         * куда карточка и ведёт.
         */
        val USAGE_RANGE = StatsRange.QUARTER

        private const val TAPS_TO_DEBUG = 5
        private const val TAPS_TO_HINT = 3
        private const val TOAST_MILLIS = 2600L
    }
}

/** Пара «значение — подпись» под полосой сжатия. */
data class ProfileFact(val value: String, val caption: String)

/** Плитка инструмента в карточке «чем вы пользовались». */
data class UsageTile(
    val kind: ProcessingKind,
    val title: String,
    val countText: String,
    val unit: String,
    /**
     * Длина полоски — доля от самого частого инструмента, а не от всех
     * операций: полоски здесь сравнивают инструменты между собой.
     */
    val share: Double,
    /**
     * Самый частый инструмент подсвечен акцентом: подсветить все — значит
     * не подсветить ни одного.
     */
    val isLeading: Boolean,
)

enum class SettingsRowId {
    LANGUAGE,
    ACCESS,
    VERSION,
}

data class SettingsRow(
    val id: SettingsRowId,
    val label: String,
    val value: String,
    /**
     * Строка без действия — просто показание: у неё нет ни нажатия,
     * ни отклика на палец.
     */
    val isTappable: Boolean,
)
