package com.slimly.feature.scan

import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.service.CameraAccess
import com.slimly.core.service.CameraFlash
import com.slimly.core.service.DocumentCameraSession
import com.slimly.core.service.DocumentCameraState
import com.slimly.feature.scan.api.ScanRoute
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ScanCameraViewModel(
    /**
     * Камера отдана экрану целиком: превью — системный `View`, и пересказывать
     * его по одному свойству значило бы дублировать всю сессию.
     */
    val camera: DocumentCameraSession,
    private val resources: Resources,
    private val formatters: SlimlyFormatters,
    private val pageLimit: Int,
    private val analytics: ScanAnalytics,
    private val router: Router,
    private val scope: CoroutineScope,
) {
    /** Снимок состояния съёмки — ровно то, что рисует экран. */
    var state: DocumentCameraState by mutableStateOf(camera.state.value)
        private set

    var toast: String? by mutableStateOf(null)
        private set

    var access: CameraAccess by mutableStateOf(camera.access())
        private set

    /** Системный запрос показываем один раз за посещение экрана. */
    var hasRequestedAccess: Boolean by mutableStateOf(false)
        private set

    /**
     * Автоспуск и вспышка живут в сессии обычными свойствами, а не в потоке
     * состояния: их меняет экран, а не камера. Здесь их зеркало — без него
     * переключатель не перерисовался бы.
     */
    var isAutomatic: Boolean by mutableStateOf(camera.isAutomatic)
        private set

    var isFlashOn: Boolean by mutableStateOf(camera.flash == CameraFlash.ON)
        private set

    /** Разрешение, без которого превью останется чёрным прямоугольником. */
    val permission: String = camera.permission

    private var toastJob: Job? = null

    // MARK: Жизненный цикл

    /**
     * Подписка на состояние съёмки. Живёт столько же, сколько открыт
     * видоискатель: камера занимает устройство, и держать её включённой
     * ради экрана под шторкой незачем.
     */
    suspend fun observe() {
        camera.state.collect { fresh ->
            val before = state.pages.size
            state = fresh
            // Про снятую страницу сообщаем отсюда, а не с кнопки затвора:
            // автоспуск снимает мимо неё, а сказать надо одно и то же.
            if (fresh.pages.size > before) say(pageCaptured(fresh.pages.size))
            fresh.failure?.let {
                camera.clearFailure()
                say(it)
            }
        }
    }

    fun start() {
        access = camera.access()
        if (access == CameraAccess.AUTHORIZED) camera.start()
    }

    fun stop() {
        camera.stop()
    }

    /** Экран показал системный запрос — второй раз показывать его незачем. */
    fun accessRequested() {
        hasRequestedAccess = true
    }

    /**
     * Система ответила на запрос.
     *
     * «Ещё не спрашивали» после ответа означает отказ: другого способа
     * отличить одно от другого у сессии нет, а здесь ответ виден напрямую.
     */
    fun accessAnswered() {
        val granted = camera.access()
        access = if (granted == CameraAccess.AUTHORIZED) granted else CameraAccess.DENIED
        if (access == CameraAccess.AUTHORIZED) camera.start()
    }

    // MARK: Состояние

    val pageCount: Int
        get() = state.pages.size

    val hasPages: Boolean
        get() = state.pages.isNotEmpty()

    val isDenied: Boolean
        get() = access == CameraAccess.DENIED

    val canShoot: Boolean
        get() = !state.isAtLimit && !state.isBusy

    // MARK: Подписи

    /**
     * Только количество: рядом стоит стопка с тем же числом, и слово
     * «снято» в неё уже вписано — на подпись остаётся полоска в семь десятков
     * точек, где длинная строка ломается пополам.
     */
    val pagesText: String
        get() = formatters.counts.string(pageCount, CountNoun.PAGE)

    /**
     * Что делать прямо сейчас. Подсказка говорит про кадр, а не про кнопки:
     * на съёмке человек смотрит в видоискатель, а не на интерфейс.
     *
     * Ступени «наводимся» здесь нет, в отличие от iOS: непрерывного сигнала
     * о наводке Android не отдаёт, и спокойствие кадра меряется одним
     * способом — рамка перестала шевелиться.
     */
    val hintText: String
        get() = when {
            state.isAtLimit -> resources.getString(R.string.camera_limit_reached, limitText)
            !isAutomatic -> resources.getString(R.string.camera_manual_hint)
            state.quad == null -> resources.getString(R.string.camera_aim_hint)
            else -> resources.getString(R.string.camera_bounds_found)
        }

    val flashLabel: String
        get() = resources.getString(if (isFlashOn) R.string.camera_flash_on else R.string.camera_flash_auto)

    val automaticLabel: String
        get() = resources.getString(if (isAutomatic) R.string.camera_auto_capture else R.string.camera_manual)

    val doneTitle: String
        get() = if (hasPages) {
            resources.getString(R.string.camera_done_count, pageCount)
        } else {
            resources.getString(R.string.camera_done)
        }

    private val limitText: String
        get() = formatters.counts.string(pageLimit, CountNoun.PAGE)

    // MARK: Управление

    fun toggleFlash() {
        isFlashOn = !isFlashOn
        camera.flash = if (isFlashOn) CameraFlash.ON else CameraFlash.AUTO
    }

    fun toggleAutomatic() {
        isAutomatic = !isAutomatic
        camera.isAutomatic = isAutomatic
    }

    fun shoot() {
        if (state.isAtLimit) {
            say(resources.getString(R.string.camera_limit_spoken, limitText))
            return
        }
        scope.launch { camera.shoot() }
    }

    // MARK: Навигация

    /**
     * Отказ от съёмки выбрасывает снятое: возвращаться к этим страницам
     * уже некуда — экран закрывается вместе со своим состоянием.
     */
    fun cancel() {
        analytics.didCancelCamera(pageCount)
        camera.stop()
        camera.discardPages()
        router.dismiss()
    }

    /**
     * Дальше страницы идут общим путём флоу, поэтому шаг с границами
     * встаёт в стек под камерой, а не внутри неё.
     */
    fun finish() {
        val pages = state.pages
        if (pages.isEmpty()) return

        analytics.didCapturePages(count = pages.size, isAutomatic = isAutomatic)
        camera.stop()

        val presenter = router.parent ?: router
        router.dismiss()
        presenter.push(ScanRoute.Pages(pages))
    }

    // MARK: Сообщения

    private fun pageCaptured(number: Int): String = resources.getString(R.string.camera_page_captured, number)

    private fun say(text: String) {
        toastJob?.cancel()
        toast = text
        toastJob = scope.launch {
            delay(TOAST_LIFETIME)
            toast = null
        }
    }

    private companion object {
        /** Столько же, сколько на iOS: успеть прочитать, но не отвлечь от кадра. */
        val TOAST_LIFETIME = 2.2.seconds
    }
}
