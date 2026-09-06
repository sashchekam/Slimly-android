package com.slimly.feature.compress.api

import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.ProcessingResult
import com.slimly.navigation.Route
import kotlinx.parcelize.Parcelize

/**
 * Публичный вход во флоу сжатия.
 *
 * Маршруты несут данные шага, а не ссылку на общий объект сессии: так
 * состояние потока целиком лежит в роутере, и любую точку флоу можно
 * восстановить одним присвоением — из теста, из ссылки, из `savedInstanceState`
 * после смерти процесса.
 */
sealed interface CompressRoute : Route {
    /** Выбор файлов — вход во флоу. */
    @Parcelize
    data object Picker : CompressRoute

    /** Степень сжатия для уже выбранных файлов. */
    @Parcelize
    data class Level(val items: List<MediaItem>) : CompressRoute

    @Parcelize
    data class Run(val request: ProcessingRequest) : CompressRoute

    @Parcelize
    data class Outcome(val outcome: ProcessingOutcome) : CompressRoute

    /** Просмотр одного готового файла — у сжатия это пара «до / после». */
    @Parcelize
    data class Preview(val result: ProcessingResult) : CompressRoute
}
