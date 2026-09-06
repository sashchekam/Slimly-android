package com.slimly.feature.audio

import android.content.res.Resources
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.AudioFormat
import com.slimly.core.model.MediaFormat
import com.slimly.core.model.MediaKind
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingTarget
import com.slimly.core.service.MediaFileSharing
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaPreviewRenderer
import com.slimly.core.service.MediaProcessingService
import com.slimly.feature.audio.api.AudioRoute
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
 * Точка сборки флоу «извлечь звук».
 *
 * От конвертации отличается сутью, а не экранами: исходное видео остаётся
 * нетронутым, а дорожка становится отдельным файлом — поэтому предложения
 * удалить оригиналы здесь нет.
 */
fun audioResolver(
    resources: Resources,
    formatters: SlimlyFormatters,
    mediaLibrary: MediaLibrary,
    processingService: MediaProcessingService,
    previewRenderer: MediaPreviewRenderer,
    sharing: MediaFileSharing,
    analytics: AnalyticsService,
): RouteResolver {
    val flow = FlowAnalytics(analytics, FlowTool.AUDIO)
    val copy = FlowCopy(resources, formatters)

    return routeResolver<AudioRoute> { route, router ->
        val scope = rememberCoroutineScope()

        when (route) {
            AudioRoute.Picker -> MediaPickerScreen(
                viewModel = remember(router) {
                    MediaPickerViewModel(
                        config = MediaPickerConfig(
                            title = resources.getString(R.string.tool_title),
                            hint = resources.getString(R.string.picker_hint),
                            kind = MediaKind.VIDEO,
                            limit = VIDEO_LIMIT,
                            noun = CountNoun.VIDEO,
                        ),
                        library = mediaLibrary,
                        formatters = formatters,
                        analytics = flow,
                        router = router,
                        scope = scope,
                        destination = { FlowDestination.Push(AudioRoute.Format(it)) },
                    )
                },
            )

            is AudioRoute.Format -> FormatStepScreen(
                viewModel = remember(route) {
                    FormatStepViewModel(
                        config = formatConfig(resources),
                        items = route.items,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        formatters = formatters,
                        router = router,
                        run = { AudioRoute.Run(it) },
                        preview = { FlowDestination.Push(AudioRoute.SourcePreview(it)) },
                    )
                },
            )

            is AudioRoute.SourcePreview -> SourcePreviewScreen(
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

            is AudioRoute.Run -> ProcessingScreen(
                viewModel = remember(route) {
                    ProcessingViewModel(
                        request = route.request,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        service = processingService,
                        formatters = formatters,
                        copy = copy,
                        analytics = flow,
                        router = router,
                        outcome = { AudioRoute.Outcome(it) },
                    )
                },
            )

            is AudioRoute.Outcome -> OutcomeScreen(
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
                        preview = { FlowDestination.Push(AudioRoute.Preview(it)) },
                    )
                },
            )

            is AudioRoute.Preview -> ResultPreviewScreen(
                viewModel = remember(route) {
                    ResultPreviewViewModel(
                        result = route.result,
                        kind = ProcessingKind.AUDIO_EXTRACTION,
                        thumbnails = mediaLibrary.thumbnailLoader,
                        // Кадра у дорожки нет, и рендерер честно вернёт `null`:
                        // на его месте останется подпись «ДОРОЖКА».
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
 * MP3 в списке нет: кодировщика MP3 в системе не существует — ни здесь,
 * ни на iOS, — и предлагать его значило бы обещать то, чего не умеет
 * платформа.
 *
 * Доли веса тут не от исходника, а от длительности: считает их само задание,
 * по битрейту дорожки. Число рядом с форматом всё равно нужно, поэтому
 * оставлено грубым ориентиром.
 */
private fun formatConfig(resources: Resources): FormatStepConfig = FormatStepConfig(
    title = resources.getString(R.string.format_title),
    note = resources.getString(R.string.format_note),
    actionTitle = resources.getString(R.string.tool_title),
    sourceFormat = resources.getString(R.string.format_source),
    formats = listOf(
        MediaFormat(target = ProcessingTarget.Audio(AudioFormat.Aac(AAC_KILOBITS)), ratio = 0.03),
        MediaFormat(target = ProcessingTarget.Audio(AudioFormat.Aac(AAC_KILOBITS_HIGH)), ratio = 0.06),
        MediaFormat(target = ProcessingTarget.Audio(AudioFormat.LinearPcm), ratio = 0.35),
    ),
    noun = CountNoun.VIDEO,
    makeOperation = { ProcessingOperation.ExtractAudio(it) },
)

/** Видео дороже фото в обработке, поэтому берём их по три за раз. */
private const val VIDEO_LIMIT = 3
private const val AAC_KILOBITS = 128
private const val AAC_KILOBITS_HIGH = 256
