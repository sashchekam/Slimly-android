package com.slimly.core.debug

import android.content.res.Resources
import com.slimly.core.storage.KeyValueStorage
import com.slimly.core.storage.value

/** Подмена отказа так, как её видит конвейер: готовой причиной или ничем. */
fun interface SimulatedFailureProviding {
    /** Причина, которой подменяется обработка. `null` — работаем по-настоящему. */
    suspend fun reason(): String?
}

/**
 * Читает подмену из того же хранилища, куда её пишет отладочное меню.
 *
 * Не сам [DebugSettings]: его состояние живёт на главном потоке, рядом
 * с экраном, а конвейер работает на своём исполнителе и в свёрнутом
 * приложении — дотянуться до экранного состояния ему нечем.
 *
 * В релизной сборке подмены нет вовсе: [isDebug] приходит из `:app`,
 * потому что `BuildConfig` есть только у него.
 */
class SimulatedFailureProvider(
    private val storage: KeyValueStorage,
    private val resources: Resources,
    private val isDebug: Boolean,
) : SimulatedFailureProviding {
    override suspend fun reason(): String? {
        if (!isDebug) return null

        return storage.value(simulatedFailureKey, SimulatedFailure.OFF).reasonRes?.let(resources::getString)
    }
}
