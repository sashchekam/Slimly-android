package com.slimly.feature.convert.api

import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import com.slimly.navigation.Route
import kotlinx.parcelize.Parcelize

/**
 * Что конвертируем. Экраны у фото и видео одни и те же, различаются только
 * выборка, форматы и подписи — поэтому это параметр маршрута, а не отдельная
 * фича.
 */
enum class ConvertKind {
    PHOTO,
    VIDEO,
}

/** Публичный вход во флоу конвертации. */
sealed interface ConvertRoute : Route {
    /** Выбор файлов — вход во флоу. */
    @Parcelize
    data class Picker(val kind: ConvertKind) : ConvertRoute

    @Parcelize
    data class Format(val kind: ConvertKind, val items: List<MediaItem>) : ConvertRoute

    /** Просмотр исходного файла с шага выбора формата. */
    @Parcelize
    data class SourcePreview(val item: MediaItem) : ConvertRoute

    @Parcelize
    data class Run(val request: ProcessingRequest) : ConvertRoute

    @Parcelize
    data class Outcome(val outcome: ProcessingOutcome) : ConvertRoute

    /** Просмотр одного готового файла с экрана итога. */
    @Parcelize
    data class Preview(val result: ProcessingResult) : ConvertRoute
}
