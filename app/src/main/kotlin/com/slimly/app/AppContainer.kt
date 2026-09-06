package com.slimly.app

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import com.slimly.core.analytics.AnalyticsLog
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.CompositeAnalytics
import com.slimly.core.analytics.LogAnalytics
import com.slimly.core.analytics.RecordingAnalytics
import com.slimly.core.debug.DebugSettings
import com.slimly.core.debug.SimulatedFailureProvider
import com.slimly.core.deeplink.DeeplinkUrl
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.remoteconfig.OverridingRemoteConfig
import com.slimly.core.remoteconfig.RemoteConfigService
import com.slimly.core.service.AppEnvironmentProvider
import com.slimly.core.service.AppEnvironmentProviding
import com.slimly.core.service.MediaFileSharing
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaPreviewRenderer
import com.slimly.core.service.MediaProcessingService
import com.slimly.core.service.OnboardingState
import com.slimly.core.service.ProcessingStatsStore
import com.slimly.core.service.StoredOnboardingState
import com.slimly.core.storage.DataStoreStorage
import com.slimly.core.storage.KeyValueStorage
import com.slimly.feature.audio.audioResolver
import com.slimly.feature.compress.compressResolver
import com.slimly.feature.convert.convertResolver
import com.slimly.feature.debug.SystemClipboard
import com.slimly.feature.debug.api.DebugRoute
import com.slimly.feature.debug.debugResolver
import com.slimly.feature.home.api.HomeRoute
import com.slimly.feature.home.homeResolver
import com.slimly.feature.onboarding.api.OnboardingRoute
import com.slimly.feature.onboarding.onboardingResolver
import com.slimly.feature.profile.api.ProfileRoute
import com.slimly.feature.profile.profileResolver
import com.slimly.feature.scan.scanResolver
import com.slimly.firebase.FirebaseServices
import com.slimly.mediakit.camera.DocumentCameras
import com.slimly.mediakit.files.MediaFileShare
import com.slimly.mediakit.library.MediaStoreLibrary
import com.slimly.mediakit.processing.MediaProcessor
import com.slimly.mediakit.processing.preview.MediaPreviews
import com.slimly.mediakit.scan.ContourBoundsDetector
import com.slimly.navigation.DeeplinkRegistry
import com.slimly.navigation.Route
import com.slimly.navigation.RouteRegistry
import com.slimly.navigation.RouteResolver
import com.slimly.navigation.Router
import com.slimly.navigation.navigate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Единственное место, которое знает про все фичи сразу: здесь живут сервисы,
 * реестр «маршрут → экран» и таблица ссылок. Фичи про контейнер не знают —
 * зависимости приходят к ним явными параметрами в `*Resolver(...)`.
 *
 * Живёт на уровне приложения, а не экрана: сервисы переживают поворот,
 * а стек навигации — нет, он лежит в роутере и сохраняется отдельно.
 */
class AppContainer(context: Context, isDebug: Boolean) {
    private val storage: KeyValueStorage = DataStoreStorage(context)

    /**
     * Форматтеры собраны один раз на приложение: каждому нужны ресурсы,
     * а экранам — готовый свёрток вместо шести параметров.
     */
    val formatters = SlimlyFormatters(context.resources)

    val mediaLibrary: MediaLibrary = MediaStoreLibrary(context)

    /**
     * Хранилище статистики создаётся здесь, а не внутри обработчика:
     * те же цифры показывает профиль, и экземпляр у них должен быть один.
     */
    private val statsStore = ProcessingStatsStore(storage)

    /**
     * Паспорт сборки: версия в профиле и хвост письма в поддержку. Живёт
     * на уровне приложения, потому что идентификатор установки он заводит
     * один раз и хранит в тех же настройках.
     */
    private val environmentProvider: AppEnvironmentProviding = AppEnvironmentProvider(context, storage)

    /**
     * Состояние отладочного меню. `null` в релизе: подменять там нечем,
     * и хранить состояние несуществующего экрана незачем.
     */
    val debugSettings: DebugSettings? = if (isDebug) DebugSettings(storage) else null

    val processingService: MediaProcessingService = MediaProcessor.of(
        context = context,
        library = mediaLibrary,
        statsStore = statsStore,
        // Подмену конвейер читает из хранилища, а не из экранного состояния
        // меню: он работает на своём исполнителе и в свёрнутом приложении.
        simulatedFailure = SimulatedFailureProvider(storage, context.resources, isDebug),
    )

    /**
     * Кадр готового файла для экранов просмотра. Отдельно от обработчика:
     * достать картинку из файла нужно и тогда, когда обрабатывать уже нечего.
     */
    private val previewRenderer: MediaPreviewRenderer = MediaPreviews()

    /**
     * Чем готовый файл выходит наружу: шторка «Поделиться» и «Открыть в…».
     * Права на файл выдаёт `FileProvider` из `:mediakit` — снаружи виден
     * только собранный `Intent`.
     */
    private val fileSharing: MediaFileSharing = MediaFileShare(context)

    /**
     * Копия отправленного на устройстве — её показывает отладочное меню.
     * В релизе журнала нет: писать в него некому и показывать негде.
     */
    val analyticsLog: AnalyticsLog? = if (isDebug) AnalyticsLog() else null

    /**
     * Общая аналитика приложения. Фича её не создаёт: она заводит свою поверх
     * этой и получает её параметром резолвера.
     */
    val analytics: AnalyticsService

    /** Значения, которыми можно управлять без выпуска сборки. */
    val remoteConfig: RemoteConfigService

    val registry: RouteRegistry

    /**
     * Показывали ли уже тур. Решение «с чего начать» принимает старт, а
     * отметку ставит последний шаг тура, — поэтому флаг живёт отдельно
     * от обоих.
     */
    private val onboarding: OnboardingState = StoredOnboardingState(storage)

    private val appAnalytics: AppAnalytics
    private val deeplinks = DeeplinkRegistry(deeplinkMappers())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Ящик для ссылок. Хранится последняя: нажатие в меню иконки одно,
     * и открыть оно должно один инструмент. Ссылка, пришедшая раньше первого
     * кадра, дожидается его здесь — открывать до появления экрана нечего.
     */
    private val inbox = Channel<DeeplinkUrl>(Channel.CONFLATED)

    val pendingDeeplinks: Flow<DeeplinkUrl> = inbox.receiveAsFlow()

    init {
        val firebase = FirebaseServices.of(context, isDebug)

        // В отладочной сборке событие дублируется в лог и в журнал: иначе
        // увидеть, что именно ушло, можно только в веб-консоли и назавтра.
        analytics = if (analyticsLog == null) {
            firebase.analytics
        } else {
            CompositeAnalytics(listOf(firebase.analytics, LogAnalytics(), RecordingAnalytics(analyticsLog)))
        }

        // Подмены отладочного меню ложатся поверх источника и действуют
        // на всё приложение: проверять флаг имеет смысл на настоящих экранах,
        // а не только на том, где его подменили.
        remoteConfig = if (isDebug) OverridingRemoteConfig(firebase.remoteConfig, storage) else firebase.remoteConfig

        appAnalytics = AppAnalytics(analytics)
        registry = RouteRegistry(
            listOf(
                homeResolver(
                    tools = { homeTools(context.resources) },
                    profile = ProfileRoute.Profile,
                    analytics = analytics,
                ),
                profileResolver(
                    resources = context.resources,
                    library = mediaLibrary,
                    service = processingService,
                    environment = environmentProvider,
                    formatters = formatters,
                    analytics = analytics,
                    // Маршрут, а не тип: профилю знать, какой модуль стоит
                    // за меню и есть ли он в сборке, незачем.
                    debugMenu = if (debugSettings == null) null else DebugRoute.Menu,
                ),
                compressResolver(
                    resources = context.resources,
                    formatters = formatters,
                    mediaLibrary = mediaLibrary,
                    processingService = processingService,
                    previewRenderer = previewRenderer,
                    sharing = fileSharing,
                    analytics = analytics,
                ),
                convertResolver(
                    resources = context.resources,
                    formatters = formatters,
                    mediaLibrary = mediaLibrary,
                    processingService = processingService,
                    previewRenderer = previewRenderer,
                    sharing = fileSharing,
                    analytics = analytics,
                ),
                onboardingResolver(state = onboarding, home = HomeRoute.Home, analytics = analytics),
                audioResolver(
                    resources = context.resources,
                    formatters = formatters,
                    mediaLibrary = mediaLibrary,
                    processingService = processingService,
                    previewRenderer = previewRenderer,
                    sharing = fileSharing,
                    analytics = analytics,
                ),
                scanResolver(
                    resources = context.resources,
                    formatters = formatters,
                    mediaLibrary = mediaLibrary,
                    processingService = processingService,
                    // Камера и поиск листа нужны одному инструменту, поэтому
                    // и собираются здесь же, а не рядом с общими сервисами.
                    cameras = DocumentCameras(context),
                    detector = ContourBoundsDetector(),
                    previewRenderer = previewRenderer,
                    sharing = fileSharing,
                    analytics = analytics,
                ),
            ) + debugResolvers(context, firebase.remoteConfig),
        )

        // Свежие значения нужны первому же экрану, но ждать их старт не должен:
        // до ответа сети работают умолчания ключей.
        scope.launch { remoteConfig.refresh() }

        startOverrides(remoteConfig)
    }

    /**
     * Отладочное меню — единственный резолвер, которого в релизе нет вовсе:
     * маршрут туда положить нечем, и разбирать его некому.
     */
    private fun debugResolvers(context: Context, source: RemoteConfigService): List<RouteResolver> {
        val settings = debugSettings ?: return emptyList()
        val log = analyticsLog ?: return emptyList()

        return listOf(
            debugResolver(
                resources = context.resources,
                settings = settings,
                statsStore = statsStore,
                onboarding = onboarding,
                // Источник без подмен поверх: экран показывает, что реально
                // приехало с сервера, и подмену рядом с ним.
                remoteConfig = source,
                analyticsLog = log,
                environment = environmentProvider,
                clipboard = SystemClipboard(context),
            ),
        )
    }

    /**
     * Поднять состояние отладочного меню и связать его с конфигом.
     *
     * Подмены доезжают до конфига потоком: меню правит своё состояние,
     * а действовать подмена должна на всём приложении сразу — иначе
     * подменённый флаг виден только на том экране, где его подменили.
     */
    private fun startOverrides(config: RemoteConfigService) {
        val settings = debugSettings ?: return

        scope.launch {
            settings.load()

            val overriding = config as? OverridingRemoteConfig ?: return@launch
            snapshotFlow { settings.remoteConfigOverrides }.collect(overriding::apply)
        }
    }

    /**
     * Корень первого кадра. Первый запуск начинается туром, все следующие —
     * главным экраном.
     */
    suspend fun startRoute(): Route = if (onboarding.hasSeenTour()) HomeRoute.Home else OnboardingRoute.Tour

    /** Ссылка пришла от системы. Разбор — дело `:core`, показ — дело роутера. */
    fun receive(deeplink: DeeplinkUrl) {
        inbox.trySend(deeplink)
    }

    /**
     * Ссылка — это одно присвоение состояния: маршруты раскладываются в корень,
     * стек и модалку, а хост дорисовывает иерархию сам.
     */
    fun open(deeplink: DeeplinkUrl, router: Router) {
        val destination = deeplinks.destination(deeplink)
        if (destination == null) {
            appAnalytics.didFailDeeplink(deeplink)
            return
        }
        router.navigate(destination)
        appAnalytics.didOpenDeeplink(deeplink, DeeplinkSource.URL)
    }
}
