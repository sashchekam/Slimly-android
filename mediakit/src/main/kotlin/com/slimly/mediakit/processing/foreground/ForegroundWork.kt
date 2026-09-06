package com.slimly.mediakit.processing.foreground

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Что держит процесс живым, пока идёт обработка.
 *
 * На iOS такого нет намеренно: ушёл с экрана — работа оборвалась. На Android
 * свернуть приложение посреди долгой работы — норма, и оборванная задача
 * читается как баг.
 */
internal interface ForegroundWorkHolding {
    fun begin()

    fun end()
}

internal class ForegroundWork(private val context: Context) : ForegroundWorkHolding {
    override fun begin() {
        try {
            ContextCompat.startForegroundService(context, intent())
        } catch (@Suppress("SwallowedException") denied: IllegalStateException) {
            // Система запрещает поднимать такой сервис из фона. Обработка
            // всё равно пойдёт — просто без уведомления и без защиты
            // от выгрузки процесса. Ронять из-за этого работу незачем.
        }
    }

    override fun end() {
        context.stopService(intent())
    }

    private fun intent() = Intent(context, ProcessingForegroundService::class.java)
}
