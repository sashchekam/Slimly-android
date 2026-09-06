package com.slimly.feature.audio.api

import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import com.slimly.navigation.Route
import kotlinx.parcelize.Parcelize

/** Публичный вход во флоу извлечения звука. */
sealed interface AudioRoute : Route {
    @Parcelize
    data object Picker : AudioRoute

    @Parcelize
    data class Format(val items: List<MediaItem>) : AudioRoute

    /** Просмотр исходного видео с шага выбора формата дорожки. */
    @Parcelize
    data class SourcePreview(val item: MediaItem) : AudioRoute

    @Parcelize
    data class Run(val request: ProcessingRequest) : AudioRoute

    @Parcelize
    data class Outcome(val outcome: ProcessingOutcome) : AudioRoute

    /** Просмотр готовой дорожки с экрана итога. */
    @Parcelize
    data class Preview(val result: ProcessingResult) : AudioRoute
}
