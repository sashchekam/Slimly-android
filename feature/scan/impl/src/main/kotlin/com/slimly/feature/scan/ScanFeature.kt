package com.slimly.feature.scan

import android.content.res.Resources
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.MediaFormat
import com.slimly.core.model.MediaKind
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingTarget
import com.slimly.core.model.ScanBounds
import com.slimly.core.model.ScanMode
import com.slimly.core.service.DocumentBoundsDetector
import com.slimly.core.service.DocumentCameraFactory
import com.slimly.core.service.MediaFileSharing
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaPreviewRenderer
import com.slimly.core.service.MediaProcessingService
import com.slimly.feature.scan.api.ScanRoute
import com.slimly.mediaflow.FlowAnalytics
import com.slimly.mediaflow.FlowCopy
import com.slimly.mediaflow.FlowDestination
import com.slimly.mediaflow.FlowTool
import com.slimly.mediaflow.format.FormatStepConfig
import com.slimly.mediaflow.format.FormatStepScreen
import com.slimly.mediaflow.format.FormatStepViewModel
import com.slimly.mediaflow.outcome.OutcomeScreen
import com.slimly.mediaflow.outcome.OutcomeViewModel
import com.slimly.mediaflow.picker.MediaPickerConfig
import com.slimly.mediaflow.picker.MediaPickerScreen
import com.slimly.mediaflow.picker.MediaPickerViewModel
import com.slimly.mediaflow.preview.ResultPreviewScreen
import com.slimly.mediaflow.preview.ResultPreviewViewModel
import com.slimly.mediaflow.preview.SourcePreviewScreen
import com.slimly.mediaflow.preview.SourcePreviewViewModel
import com.slimly.mediaflow.run.ProcessingScreen
import com.slimly.mediaflow.run.ProcessingViewModel
import com.slimly.mediaflow.thumbnailLoader
import com.slimly.navigation.RouteResolver
import com.slimly.navigation.routeResolver

/**
 * Точка сборки флоу скана документа.
 *
 * Единственный инструмент с собственным началом: у скана есть выбор
 * источника — камера или галерея, — а дальше начинается общий путь.
 */
// LongMethod: тело резолвера — таблица «маршрут → экран», по ветке
// на маршрут. Резать её на функции значило бы разложить одну таблицу
// по файлу и потерять то, ради чего она читается сверху вниз.
@Suppress("LongParameterList", "LongMethod")
fun scanResolver(
    resources: Resources,
    formatters: SlimlyFormatters,
    mediaLibrary: MediaLibrary,
    processingService: MediaProcessingService,
    /**
     * Камера и поиск листа приходят контрактами: системный медиастек знает
     * один модуль, и фича к нему не относится.
     */
    cameras: DocumentCameraFactory,
    detector: DocumentBoundsDetector,
    previewRenderer: MediaPreviewRenderer,
    sharing: MediaFileSharing,
    analytics: AnalyticsService,
): RouteResolver {
    // Путь обработки у скана общий с остальными инструментами, а съёмка
    // страниц и границы листа — только его: отсюда две аналитики.
    val flow = FlowAnalytics(analytics, FlowTool.SCAN)
    val scan = ScanAnalytics(analytics)
    val copy = FlowCopy(resources, formatters)

    return routeResolver<ScanRoute> { route, router ->
        val scope = rememberCoroutineScope()

        when (route) {
            ScanRoute.Intro -> ScanIntroSheet(
                viewModel = remember(router) { ScanIntroViewModel(analytics = scan, router = router) },
            )

            ScanRoute.Camera -> ScanCameraScreen(
                viewModel = remember(router) {
                    ScanCameraViewModel(
                        camera = cameras.session(PAGE_LIMIT),
                        resources = resources,
                        formatters = formatters,
                        pageLimit = PAGE_LIMIT,
                        analytics = scan,
                        router = router,
                        scope = scope,
                    )
                },
            )

            ScanRoute.Picker -> MediaPickerScreen(
                viewModel = remember(router) {
                    MediaPickerViewModel(
                        config = MediaPickerConfig(
                            title = resources.getString(R.string.picker_title),
                            hint = resources.getString(R.string.picker_hint),
                            kind = MediaKind.PHOTO,
                            limit = PAGE_LIMIT,
                            noun = CountNoun.PAGE,
                            // Страницу выбирают по тому, что на ней написано:
                            // вес снимка здесь ни на что не влияет и в итог
                            // документа не переходит.
                            showsSize = false,
                        ),
                        library = mediaLibrary,
                        formatters = formatters,
                        analytics = flow,
                        router = router,
                        scope = scope,
                        destination = { FlowDestination.Push(ScanRoute.Pages(it)) },
                    )
                },
            )

            is ScanRoute.Pages -> ScanPagesScreen(
                viewModel = remember(route) {
                    ScanPagesViewModel(
                        items = route.items,
                        resources = resources,
                        formatters = formatters,
                        detector = detector,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        analytics = scan,
                        router = router,
                    )
                },
            )

            is ScanRoute.Format -> FormatStepScreen(
                viewModel = remember(route) {
                    FormatStepViewModel(
                        config = formatConfig(resources, route.bounds),
                        items = route.items,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        formatters = formatters,
                        router = router,
                        run = { ScanRoute.Run(it) },
                        preview = { FlowDestination.Push(ScanRoute.SourcePreview(it)) },
                    )
                },
            )

            is ScanRoute.SourcePreview -> SourcePreviewScreen(
                viewModel = remember(route) {
                    SourcePreviewViewModel(
                        item = route.item,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        formatters = formatters,
                        resources = resources,
                        router = router,
                        // Как и в списке страниц: вес снимка на документ
                        // не влияет и в его итог не переходит.
                        showsSize = false,
                    )
                },
            )

            is ScanRoute.Run -> ProcessingScreen(
                viewModel = remember(route) {
                    ProcessingViewModel(
                        request = route.request,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        service = processingService,
                        formatters = formatters,
                        copy = copy,
                        analytics = flow,
                        router = router,
                        outcome = { ScanRoute.Outcome(it) },
                    )
                },
            )

            is ScanRoute.Outcome -> OutcomeScreen(
                viewModel = remember(route) {
                    OutcomeViewModel(
                        outcome = route.outcome,
                        library = mediaLibrary,
                        service = processingService,
                        formatters = formatters,
                        resources = resources,
                        copy = copy,
                        analytics = flow,
                        router = router,
                        scope = scope,
                        sharing = sharing,
                        preview = { FlowDestination.Push(ScanRoute.Preview(it)) },
                    )
                },
            )

            is ScanRoute.Preview -> ResultPreviewScreen(
                viewModel = remember(route) {
                    ResultPreviewViewModel(
                        result = route.result,
                        kind = ProcessingKind.SCAN,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        previews = previewRenderer,
                        sharing = sharing,
                        formatters = formatters,
                        resources = resources,
                        copy = copy,
                        router = router,
                    )
                },
            )
        }
    }
}

/**
 * Режим — это вид страницы: у трёх вариантов одна и та же обработка,
 * но разное количество цвета на выходе.
 *
 * Границы приезжают с предыдущего шага и уходят в задание: обработка
 * не должна искать лист заново — рамку человек уже видел и принял.
 */
private fun formatConfig(resources: Resources, bounds: ScanBounds): FormatStepConfig = FormatStepConfig(
    title = resources.getString(R.string.format_title),
    note = resources.getString(R.string.format_note),
    actionTitle = resources.getString(R.string.format_action),
    sourceFormat = resources.getString(R.string.format_source),
    formats = ScanMode.entries.map { MediaFormat(target = ProcessingTarget.Document(it)) },
    noun = CountNoun.PAGE,
    // У скана результат ценен читаемостью, а не мегабайтами: прогноз
    // «5 МБ → ≈ 0,1 МБ» здесь ничего не решает.
    showsSize = false,
    makeOperation = { ProcessingOperation.Scan(format = it, bounds = bounds) },
)

/**
 * Сколько страниц берём за раз — и в галерее, и с камеры.
 *
 * Предел один на оба источника: это ограничение документа, а не способа
 * его набрать, и расходиться этим числам было бы неоткуда объяснить.
 */
internal const val PAGE_LIMIT = 5
