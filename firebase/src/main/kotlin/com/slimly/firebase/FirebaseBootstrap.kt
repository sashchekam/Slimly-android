package com.slimly.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Поднять SDK. Отдельным воркером, потому что «поднялся или нет» решает
 * состав сервисов: без конфигурации ни аналитики, ни удалённого конфига
 * не будет, и узнать об этом надо один раз на старте.
 */
internal fun interface FirebaseBootstrapping {
    /** `false` — SDK не сконфигурирован, обращаться к нему нельзя. */
    fun start(context: Context): Boolean
}

/**
 * `initializeApp` берёт ключи из ресурсов, которые генерирует плагин
 * `google-services` по `google-services.json`. Файла в репозитории нет,
 * плагин не подключён — и это рабочее состояние, а не поломка: клон
 * на чужой машине должен собираться и запускаться, просто без аналитики.
 */
internal class FirebaseBootstrap : FirebaseBootstrapping {
    override fun start(context: Context): Boolean {
        // Повторный вызов SDK переживает: `FirebaseApp` живёт один на процесс,
        // и второй раз возвращается уже созданный.
        val app = FirebaseApp.initializeApp(context)
        if (app == null) {
            Log.w(TAG, "google-services.json не подключён — Firebase выключен")
        }
        return app != null
    }

    private companion object {
        const val TAG = "slimly.firebase"
    }
}
