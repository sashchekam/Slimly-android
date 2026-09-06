package com.slimly.mediakit.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.view.View
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.slimly.core.service.CameraAccess
import com.slimly.core.service.CameraFlash
import com.slimly.core.service.DocumentCameraSession
import com.slimly.core.service.DocumentCameraState
import com.slimly.mediakit.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Камера скана на CameraX.
 *
 * Сама не считает и не пишет: разбор кадра, решение автоспуска и хранение
 * снятого — три отдельные работы, а здесь остаётся то, ради чего сессию
 * и заводят: держать контроллер камеры и сводить ответы воркеров в одно
 * состояние экрана.
 *
 * Превью и анализ идут в одних и тех же пропорциях 4:3 намеренно: рамка
 * рисуется в долях кадра анализа, и разойдись их поля зрения — она обещала
 * бы обрезку по границам, которых на превью не видно.
 */
internal class CameraXSession(
    context: Context,
    private val resources: Resources,
    private val pageLimit: Int,
    private val pages: CapturedPageStoring,
    private val frames: CameraFrameReading = CameraFrameReader(),
    private val shutter: AutoShutterDeciding = AutoShutter(),
) : DocumentCameraSession {
    override val permission: String = Manifest.permission.CAMERA

    private val current = MutableStateFlow(DocumentCameraState())

    override val state: StateFlow<DocumentCameraState> = current.asStateFlow()

    override var isAutomatic: Boolean = true

    override var flash: CameraFlash = CameraFlash.AUTO
        set(value) {
            field = value
            controller.imageCaptureFlashMode =
                if (value == CameraFlash.ON) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_AUTO
        }

    /** Съёмка и автоспуск живут на главном потоке: их видит экран. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val owner = SessionLifecycle()
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val appContext = context.applicationContext

    /** Когда разбирали кадр в последний раз — по нему идёт прореживание. */
    private var readAt = 0L

    private var isBound = false

    private val controller = LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
        val ratio = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .build()
        previewResolutionSelector = ratio
        imageAnalysisResolutionSelector = ratio
        imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        // Кадр анализа в RGBA разбирается в растр одним копированием,
        // без ручной сборки трёх плоскостей YUV.
        imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        setImageAnalysisAnalyzer(Dispatchers.Default.asExecutor()) { image -> image.use(::handle) }
    }

    override fun access(): CameraAccess =
        if (ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED) {
            CameraAccess.AUTHORIZED
        } else {
            // «Ещё не спрашивали» от «отказано» без Activity не отличить —
            // как и у медиатеки. Отказ проставит экран, увидев ответ.
            CameraAccess.NOT_DETERMINED
        }

    /**
     * Превью вписываем в кадр целиком, а не заполняем им экран: рамка лежит
     * в долях кадра, и обрезанное по краям превью увело бы её углы за экран.
     */
    override fun previewView(context: Context): View = PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FIT_CENTER
        controller = this@CameraXSession.controller
    }

    override fun start() {
        if (access() != CameraAccess.AUTHORIZED) return

        // Страницы прошлой съёмки к этому моменту либо собраны в документ,
        // либо выброшены: чистим папку только когда набор пуст.
        if (current.value.pages.isEmpty()) pages.clear()

        if (!isBound) {
            runCatching { controller.bindToLifecycle(owner) }
                .onSuccess { isBound = true }
                .onFailure { current.update { it.copy(failure = string(R.string.failure_camera_unavailable)) } }
        }
        owner.start()
    }

    override fun stop() {
        owner.stop()
        shutter.reset()
        current.update { it.copy(quad = null) }
    }

    override suspend fun shoot() {
        capture()
    }

    override fun discardPages() {
        pages.discard(current.value.pages)
        shutter.reset()
        current.update { it.copy(pages = emptyList(), isAtLimit = false) }
    }

    override fun clearFailure() {
        current.update { it.copy(failure = null) }
    }

    // MARK: Кадр

    /**
     * Разбираем не каждый кадр: поток идёт втрое чаще, чем меняется картинка
     * перед камерой, и лишние разборы отняли бы процессор у автофокуса.
     */
    private fun handle(image: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - readAt < FRAME_INTERVAL_MILLIS) return
        readAt = now

        val frame = frames.read(image)
        current.update { it.copy(quad = frame.quad, aspectRatio = frame.aspectRatio) }

        val isReady = isAutomatic && !current.value.isBusy && !current.value.isAtLimit
        if (isReady && shutter.shouldShoot(frame.quad, now)) {
            scope.launch { capture() }
        } else if (!isReady) {
            shutter.reset()
        }
    }

    // MARK: Съёмка

    private suspend fun capture() {
        val snapshot = current.value
        if (!isBound || snapshot.isBusy || snapshot.isAtLimit) return
        current.update { it.copy(isBusy = true) }

        try {
            val file = pages.makeFile()
            write(file)
            val page = pages.page(file, current.value.pages.size + 1)
            current.update {
                val taken = it.pages + page
                it.copy(pages = taken, isAtLimit = taken.size >= pageLimit)
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            // Ловим всё подряд: чем именно ответит камера чужого устройства,
            // заранее не перечислить, а съёмку это прерывать не должно.
            current.update { it.copy(failure = string(R.string.failure_capture_failed, error.message.orEmpty())) }
        } finally {
            current.update { it.copy(isBusy = false) }
        }
    }

    private suspend fun write(file: File) = suspendCancellableCoroutine { continuation ->
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        controller.takePicture(
            options,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    continuation.resume(Unit)
                }

                override fun onError(error: ImageCaptureException) {
                    continuation.resumeWithException(error)
                }
            },
        )
    }

    private fun string(id: Int, argument: String? = null): String =
        argument?.let { resources.getString(id, it) } ?: resources.getString(id)

    private companion object {
        /** Шесть разборов в секунду — столько же, сколько на iOS. */
        const val FRAME_INTERVAL_MILLIS = 160L
    }
}
