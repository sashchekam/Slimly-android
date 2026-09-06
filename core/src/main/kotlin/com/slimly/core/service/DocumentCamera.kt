package com.slimly.core.service

import android.content.Context
import android.view.View
import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.MediaItem
import kotlinx.coroutines.flow.StateFlow

/** Что система разрешила делать с камерой. */
enum class CameraAccess {
    NOT_DETERMINED,
    DENIED,
    AUTHORIZED,
}

/**
 * Вспышка. Выключенного положения нет намеренно: «Авто» на свету её и так
 * не зажигает, а третий пункт в переключателе пришлось бы объяснять.
 */
enum class CameraFlash {
    AUTO,
    ON,
}

/**
 * Состояние съёмки — ровно то, что рисует экран камеры.
 *
 * Одним значением, а не россыпью свойств: кадры идут несколько раз
 * в секунду, и подписка на общий снимок состояния перерисовывает экран
 * один раз на кадр, а не по разу на каждое изменившееся поле.
 *
 * Признака «камера наводится» здесь нет, в отличие от iOS: непрерывного
 * сигнала о наводке Android не отдаёт — есть только результат касания
 * по кадру. Поэтому спокойствие кадра меряется одним способом: рамка
 * перестала шевелиться.
 */
data class DocumentCameraState(
    /** Снятые страницы в порядке съёмки. */
    val pages: List<MediaItem> = emptyList(),
    /** Границы листа в живом кадре. `null` — листа не видно. */
    val quad: DocumentQuad? = null,
    /** Пропорции кадра — по ним рамка ложится поверх превью. */
    val aspectRatio: Float = DEFAULT_ASPECT_RATIO,
    /** Затвор занят: пока кадр пишется, второй не снимаем. */
    val isBusy: Boolean = false,
    /** Страниц набрано столько, сколько документ берёт за раз. */
    val isAtLimit: Boolean = false,
    /** Причина последнего отказа. Одноразовая: показавший её гасит. */
    val failure: String? = null,
)

/** Пропорции кадра до первого снимка: обычный портретный кадр камеры. */
private const val DEFAULT_ASPECT_RATIO = 3f / 4f

/**
 * Съёмка страниц документа.
 *
 * Страницы держит сессия, а не экран: предел набора относится к съёмке —
 * автоспуск обязан о нём знать, — и разъезжаться этим двум счётчикам было бы
 * неоткуда объяснить.
 *
 * Разрешение сессия только называет, как и медиатека: показать системный
 * запрос может лишь Activity, а сессия живёт на уровне приложения.
 */
interface DocumentCameraSession {
    /** Разрешение, без которого превью останется чёрным прямоугольником. */
    val permission: String

    val state: StateFlow<DocumentCameraState>

    /** Автоспуск: кадр уходит сам, как только рамка перестаёт шевелиться. */
    var isAutomatic: Boolean

    var flash: CameraFlash

    /**
     * Проверка разрешения, а не запрос: ответ система знает сразу, и ждать
     * его, в отличие от запроса к медиатеке, не приходится.
     */
    fun access(): CameraAccess

    /**
     * Живой кадр обычным `View`.
     *
     * Compose оборачивает его `AndroidView` — так же, как на iOS слой
     * превью приезжает в SwiftUI через `UIViewRepresentable`. Камеру
     * за эту границу не пускаем: системный медиастек знает один модуль.
     */
    fun previewView(context: Context): View

    /** Пустить кадр. Без разрешения превью останется чёрным. */
    fun start()

    fun stop()

    /** Снять страницу по нажатию затвора. */
    suspend fun shoot()

    /**
     * Выбросить снятое вместе с файлами: страницы, от которых отказались,
     * не должны копиться в кэше до перезапуска.
     */
    fun discardPages()

    fun clearFailure()
}

/**
 * Откуда берётся сессия. Экран заказывает её на время съёмки: камера живёт
 * ровно столько, сколько открыт видоискатель.
 */
fun interface DocumentCameraFactory {
    fun session(pageLimit: Int): DocumentCameraSession
}
