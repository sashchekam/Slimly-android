package com.slimly.mediakit.processing.foreground

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.slimly.core.service.ProcessingHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Держит процесс живым, пока идёт обработка, и показывает её в шторке.
 *
 * Сам ничего не обрабатывает: работа живёт в приложении и переживёт этот
 * сервис. Его дело — сказать системе, что процесс занят, и рисовать прогресс,
 * пока приложения не видно.
 */
class ProcessingForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notifications by lazy { ProcessingNotifications(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val processing = (application as? ProcessingHost)?.processing
        if (processing == null) {
            // Приложение собрано иначе — держать процесс не для кого.
            stopSelf()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notifications.notification(processing.active.value), type)

        scope.launch {
            processing.active.collect { run ->
                when {
                    run == null -> stopSelf()

                    // Без разрешения уведомление не покажут, а сервис всё
                    // равно работает: молча пропускаем показ вместо отказа
                    // от работы.
                    canNotify -> NotificationManagerCompat.from(this@ProcessingForegroundService)
                        .notify(NOTIFICATION_ID, notifications.notification(run))
                }
            }
        }

        // Перезапускать себя после убийства процесса нечего: работа жила
        // в памяти приложения и вместе с ним же и пропала.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * `mediaProcessing` — тип ровно про наш случай, но он появился только
     * в Android 15. До него ближайшее по смыслу — `dataSync`.
     */
    private val type: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }

    private val canNotify: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val NOTIFICATION_ID = 1
    }
}
