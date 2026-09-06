package com.slimly.feature.scan.api

import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import com.slimly.core.model.ScanBounds
import com.slimly.navigation.Route
import kotlinx.parcelize.Parcelize

/** Публичный вход во флоу скана документа. */
sealed interface ScanRoute : Route {
    /** Откуда берём страницы: камера или галерея. */
    @Parcelize
    data object Intro : ScanRoute

    /**
     * Съёмка страниц. Показывается поверх флоу, а не в стеке: у камеры
     * свой полноэкранный мир без шапки и полей.
     */
    @Parcelize
    data object Camera : ScanRoute

    @Parcelize
    data object Picker : ScanRoute

    /** Границы страниц: что нашла автоматика и что поправил человек. */
    @Parcelize
    data class Pages(val items: List<MediaItem>) : ScanRoute

    @Parcelize
    data class Format(val items: List<MediaItem>, val bounds: ScanBounds) : ScanRoute

    /** Просмотр страницы до сборки документа. */
    @Parcelize
    data class SourcePreview(val item: MediaItem) : ScanRoute

    @Parcelize
    data class Run(val request: ProcessingRequest) : ScanRoute

    @Parcelize
    data class Outcome(val outcome: ProcessingOutcome) : ScanRoute

    /** Просмотр собранного документа с экрана итога. */
    @Parcelize
    data class Preview(val result: ProcessingResult) : ScanRoute
}
