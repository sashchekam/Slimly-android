package com.slimly.mediakit.processing.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.slimly.core.model.ProcessingRun
import com.slimly.mediakit.R
import kotlin.math.roundToInt

/** Уведомление идущей обработки. */
internal fun interface ProcessingNotifying {
    /** `null` — работа только начата, и о ней ещё нечего сказать. */
    fun notification(run: ProcessingRun?): Notification
}

/**
 * Канал заводится один раз на процесс: система его запомнит, а повторный
 * вызов ничего не меняет.
 *
 * Нажатие возвращает в приложение — через системный пусковой Intent,
 * а не через имя Activity: медиа-модуль про экраны приложения не знает
 * и знать не должен.
 */
internal class ProcessingNotifications(private val context: Context) : ProcessingNotifying {
    init {
        context.getSystemService<NotificationManager>()?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel),
                // Тихий канал: обработку человек запустил сам и смотрит
                // на неё же — звонить ему об этом незачем.
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    override fun notification(run: ProcessingRun?): Notification {
        val percent = run?.let { (it.progress.overall * PERCENT).roundToInt() } ?: 0

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_processing)
            .setContentTitle(title(run))
            .setContentText(context.getString(R.string.notification_progress, percent))
            .setProgress(PERCENT, percent, run == null)
            .setOngoing(true)
            .setContentIntent(openApp())
            .setSilent(true)
            .build()
    }

    private fun title(run: ProcessingRun?): String = run
        ?.let { context.getString(it.request.kind.titleRes) }
        ?: context.getString(R.string.notification_working)

    private fun openApp(): PendingIntent? {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    private companion object {
        const val CHANNEL_ID = "processing"
        const val PERCENT = 100
    }
}
