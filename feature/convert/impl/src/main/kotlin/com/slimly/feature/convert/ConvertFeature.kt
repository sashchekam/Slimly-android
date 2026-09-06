package com.slimly.feature.convert

import android.content.res.Resources
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.service.MediaFileSharing
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaPreviewRenderer
import com.slimly.core.service.MediaProcessingService
import com.slimly.feature.convert.api.ConvertKind
import com.slimly.feature.convert.api.ConvertRoute
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
 * Точка сборки флоу конвертации фото и видео.
 *
 * Своего кода у фичи нет вовсе: все четыре шага общие, а фича задаёт только
 * их состав — какие файлы показывать, какие форматы предлагать и что писать
 * на кнопке.
 */
fun convertResolver(
    resources: Resources,
    formatters: SlimlyFormatters,
    mediaLibrary: MediaLibrary,
    processingService: MediaProcessingService,
    previewRenderer: MediaPreviewRenderer,
    sharing: MediaFileSharing,
    analytics: AnalyticsService,
): RouteResolver {
    val copy = FlowCopy(resources, formatters)

    return routeResolver<ConvertRoute> { route, router ->
        val scope = rememberCoroutineScope()

        when (route) {
            is ConvertRoute.Picker -> MediaPickerScreen(
                viewModel = remember(route) {
                    MediaPickerViewModel(
                        config = pickerConfig(resources, route.kind),
                        library = mediaLibrary,
                        formatters = formatters,
                        analytics = FlowAnalytics(analytics, tool(route.kind)),
                        router = router,
                        scope = scope,
                        destination = { FlowDestination.Push(ConvertRoute.Format(route.kind, it)) },
                    )
                },
            )

            is ConvertRoute.Format -> FormatStepScreen(
                viewModel = remember(route) {
                    FormatStepViewModel(
                        config = formatConfig(resources, route.kind, route.items),
                        items = route.items,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        formatters = formatters,
                        router = router,
                        run = { ConvertRoute.Run(it) },
                        preview = { FlowDestination.Push(ConvertRoute.SourcePreview(it)) },
                    )
                },
            )

            is ConvertRoute.SourcePreview -> SourcePreviewScreen(
                viewModel = remember(route) {
                    SourcePreviewViewModel(
                        item = route.item,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        formatters = formatters,
                        resources = resources,
                        router = router,
                    )
                },
            )

            is ConvertRoute.Run -> ProcessingScreen(
                viewModel = remember(route) {
                    ProcessingViewModel(
                        request = route.request,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        service = processingService,
                        formatters = formatters,
                        copy = copy,
                        analytics = FlowAnalytics(analytics, tool(route.request.items)),
                        router = router,
                        outcome = { ConvertRoute.Outcome(it) },
                    )
                },
            )

            is ConvertRoute.Outcome -> OutcomeScreen(
                viewModel = remember(route) {
                    OutcomeViewModel(
                        outcome = route.outcome,
                        library = mediaLibrary,
                        service = processingService,
                        formatters = formatters,
                        resources = resources,
                        copy = copy,
                        analytics = FlowAnalytics(analytics, tool(route.outcome.request.items)),
                        router = router,
                        scope = scope,
                        sharing = sharing,
                        preview = { FlowDestination.Push(ConvertRoute.Preview(it)) },
                    )
                },
            )

            is ConvertRoute.Preview -> ResultPreviewScreen(
                viewModel = remember(route) {
                    ResultPreviewViewModel(
                        result = route.result,
                        kind = ProcessingKind.CONVERSION,
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
 * Фото и видео в отчёте разъезжаются на два инструмента: экраны у них общие,
 * а привычки и деньги разные.
 */
private fun tool(kind: ConvertKind): FlowTool =
    if (kind == ConvertKind.PHOTO) FlowTool.CONVERT_PHOTO else FlowTool.CONVERT_VIDEO

/**
 * Последние шаги флоу вида уже не несут — маршрут там носит задание.
 * Что именно конвертировали, видно по файлам этого задания.
 */
private fun tool(items: List<MediaItem>): FlowTool =
    if (items.firstOrNull()?.kind == MediaKind.VIDEO) FlowTool.CONVERT_VIDEO else FlowTool.CONVERT_PHOTO

private fun pickerConfig(resources: Resources, kind: ConvertKind): MediaPickerConfig = when (kind) {
    ConvertKind.PHOTO -> MediaPickerConfig(
        title = resources.getString(R.string.photo_title),
        hint = resources.getString(R.string.photo_hint),
        kind = MediaKind.PHOTO,
        limit = PHOTO_LIMIT,
        noun = CountNoun.PHOTO,
    )

    ConvertKind.VIDEO -> MediaPickerConfig(
        title = resources.getString(R.string.video_title),
        hint = resources.getString(R.string.video_hint),
        kind = MediaKind.VIDEO,
        limit = VIDEO_LIMIT,
        noun = CountNoun.VIDEO,
    )
}

private fun formatConfig(resources: Resources, kind: ConvertKind, items: List<MediaItem>): FormatStepConfig =
    when (kind) {
        ConvertKind.PHOTO -> FormatStepConfig(
            title = resources.getString(R.string.photo_format_title),
            note = resources.getString(R.string.photo_format_note),
            actionTitle = resources.getString(R.string.convert_action),
            sourceFormat = sourceFormat(
                names = items.map(::photoFormat),
                fallback = resources.getString(R.string.photo_format_source),
            ),
            formats = PHOTO_FORMATS,
            noun = CountNoun.PHOTO,
            makeOperation = { ProcessingOperation.Convert(it) },
        )

        ConvertKind.VIDEO -> FormatStepConfig(
            title = resources.getString(R.string.video_format_title),
            note = resources.getString(R.string.video_format_note),
            actionTitle = resources.getString(R.string.convert_action),
            // Кодек исходника до чтения файла неизвестен, поэтому слева
            // показываем контейнер — то, что видно по имени файла.
            sourceFormat = sourceFormat(
                names = items.map { it.fileExtension.uppercase() },
                fallback = resources.getString(R.string.video_format_source),
            ),
            formats = VIDEO_FORMATS,
            noun = CountNoun.VIDEO,
            makeOperation = { ProcessingOperation.Convert(it) },
        )
    }

/** Сколько файлов берём за раз. У видео предел ниже: работа дороже. */
private const val PHOTO_LIMIT = 5
private const val VIDEO_LIMIT = 3
