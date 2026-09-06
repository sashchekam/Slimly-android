package com.slimly.mediakit.camera

import android.content.Context
import com.slimly.core.service.DocumentCameraFactory
import com.slimly.core.service.DocumentCameraSession
import java.io.File

/**
 * Откуда берутся сессии камеры.
 *
 * Фабрика, а не одна сессия на приложение: камера занимает устройство,
 * и держать её включённой ради экрана, который сейчас не открыт, незачем.
 */
class DocumentCameras(private val context: Context) : DocumentCameraFactory {
    override fun session(pageLimit: Int): DocumentCameraSession = CameraXSession(
        context = context,
        resources = context.resources,
        pageLimit = pageLimit,
        pages = CapturedPages(File(context.cacheDir, PAGES_FOLDER)),
    )

    private companion object {
        const val PAGES_FOLDER = "slimly-scan"
    }
}
