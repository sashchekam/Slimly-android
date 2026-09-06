package com.slimly.app

import android.app.Application
import com.slimly.core.service.MediaProcessingService
import com.slimly.core.service.ProcessingHost

/**
 * Composition root живёт здесь, а не в Activity: сервисы переживают поворот
 * экрана и смерть Activity, а собираются один раз на процесс.
 *
 * [ProcessingHost] — единственная дверь для системных сервисов: их создаёт
 * система, конструктор им не передашь, и найти обработчик они могут только
 * через `Application`.
 */
class SlimlyApplication :
    Application(),
    ProcessingHost {
    val container: AppContainer by lazy { AppContainer(this, BuildConfig.DEBUG) }

    override val processing: MediaProcessingService
        get() = container.processingService
}
