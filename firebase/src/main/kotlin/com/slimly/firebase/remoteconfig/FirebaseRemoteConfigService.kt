package com.slimly.firebase.remoteconfig

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.slimly.core.remoteconfig.RemoteConfigService
import kotlinx.coroutines.tasks.await

/**
 * Удалённый конфиг на Firebase.
 *
 * Умолчания в SDK не регистрируются: они живут в самих ключах
 * (`RemoteConfigKey`), и «значения нет» здесь — это просто `null`.
 * Иначе одно и то же умолчание пришлось бы держать в двух местах.
 */
class FirebaseRemoteConfigService(
    isDebug: Boolean,
    private val config: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) : RemoteConfigService {
    init {
        if (isDebug) {
            // По умолчанию SDK держит кэш двенадцать часов: правку в веб-консоли
            // до конца этого срока в отладочной сборке просто не увидеть.
            config.setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build(),
            )
        }
    }

    /**
     * Только загруженные значения: умолчания в SDK не регистрируются,
     * и всё, что здесь перечислено, действительно пришло с сервера.
     */
    override val names: List<String>
        get() = config.getKeysByPrefix("").sorted()

    override fun text(name: String): String? {
        val value = config.getValue(name)
        // «Статический» источник — ключа нет ни в загруженном конфиге,
        // ни в умолчаниях SDK: пустая строка тут значила бы «значение задано
        // пустым».
        if (value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC) return null

        return value.asString().takeIf { it.isNotEmpty() }
    }

    override suspend fun refresh() {
        // Молча: экран показывает то, что уже есть, а не ошибку сети.
        runCatching { config.fetchAndActivate().await() }
            .onSuccess { Log.d(TAG, "конфиг обновлён: $it") }
            .onFailure { Log.w(TAG, "конфиг не обновился: ${it.message}") }
    }

    private companion object {
        const val TAG = "slimly.remote-config"
    }
}
