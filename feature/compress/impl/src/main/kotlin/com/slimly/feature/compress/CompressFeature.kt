package com.slimly.feature.compress

import android.content.res.Resources
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.ProcessingKind
import com.slimly.core.service.MediaFileSharing
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaPreviewRenderer
import com.slimly.core.service.MediaProcessingService
import com.slimly.feature.compress.api.CompressRoute
import com.slimly.mediaflow.FlowAnalytics
import com.slimly.mediaflow.FlowCopy
import com.slimly.mediaflow.FlowDestination
import com.slimly.mediaflow.FlowTool
import com.slimly.mediaflow.outcome.OutcomeScreen
import com.slimly.mediaflow.outcome.OutcomeViewModel
import com.slimly.mediaflow.picker.MediaPickerConfig
import com.slimly.mediaflow.picker.MediaPickerScreen
import com.slimly.mediaflow.picker.MediaPickerViewModel
import com.slimly.mediaflow.preview.ResultPreviewScreen
import com.slimly.mediaflow.preview.ResultPreviewViewModel
import com.slimly.mediaflow.run.ProcessingScreen
import com.slimly.mediaflow.run.ProcessingViewModel
import com.slimly.mediaflow.thumbnailLoader
import com.slimly.navigation.RouteResolver
import com.slimly.navigation.routeResolver

/**
 * Точка сборки флоу сжатия.
 *
 * Шаги выбора, обработки и итога — общие для всех инструментов, поэтому
 * собственного кода у фичи ровно столько, сколько в ней своего: шторка
 * со степенью сжатия.
 */
fun compressResolver(
    resources: Resources,
    formatters: SlimlyFormatters,
    mediaLibrary: MediaLibrary,
    processingService: MediaProcessingService,
    previewRenderer: MediaPreviewRenderer,
    sharing: MediaFileSharing,
    analytics: AnalyticsService,
): RouteResolver {
    // Общая аналитика приложения складывается в два объекта: путь обработки
    // у инструментов один на всех, а степень сжатия — своя.
    val flow = FlowAnalytics(analytics, FlowTool.COMPRESS)
    val compress = CompressAnalytics(analytics)
    val copy = FlowCopy(resources, formatters)

    return routeResolver<CompressRoute> { route, router ->
        val scope = rememberCoroutineScope()

        when (route) {
            CompressRoute.Picker -> MediaPickerScreen(
                viewModel = remember(router) {
                    MediaPickerViewModel(
                        config = MediaPickerConfig(
                            title = resources.getString(R.string.picker_title),
                            hint = resources.getString(R.string.picker_hint),
                            // Ограничения по виду файла нет: сжимаются и фото,
                            // и видео, и брать их можно вперемешку.
                            kind = null,
                            limit = FREE_LIMIT,
                            noun = CountNoun.FILE,
                        ),
                        library = mediaLibrary,
                        formatters = formatters,
                        analytics = flow,
                        router = router,
                        scope = scope,
                        // Степень сжатия — шторка поверх выбора: настройка
                        // одна, и ради неё уходить с экрана не стоит.
                        destination = { FlowDestination.Present(CompressRoute.Level(it)) },
                    )
                },
            )

            is CompressRoute.Level -> LevelSheet(
                viewModel = remember(route) {
                    LevelSheetViewModel(
                        items = route.items,
                        formatters = formatters,
                        analytics = compress,
                        router = router,
                    )
                },
            )

            is CompressRoute.Run -> ProcessingScreen(
                viewModel = remember(route) {
                    ProcessingViewModel(
                        request = route.request,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        service = processingService,
                        formatters = formatters,
                        copy = copy,
                        analytics = flow,
                        router = router,
                        outcome = { CompressRoute.Outcome(it) },
                    )
                },
            )

            is CompressRoute.Outcome -> OutcomeScreen(
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
                        preview = { FlowDestination.Push(CompressRoute.Preview(it)) },
                    )
                },
            )

            is CompressRoute.Preview -> ResultPreviewScreen(
                viewModel = remember(route) {
                    ResultPreviewViewModel(
                        result = route.result,
                        kind = ProcessingKind.COMPRESSION,
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

/** Сколько файлов берём за раз — предел бесплатной обработки. */
private const val FREE_LIMIT = 5
