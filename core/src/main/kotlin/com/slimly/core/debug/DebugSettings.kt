package com.slimly.core.debug

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.R
import com.slimly.core.remoteconfig.RemoteConfigOverrides
import com.slimly.core.remoteconfig.remoteConfigOverridesKey
import com.slimly.core.storage.KeyValueStorage
import com.slimly.core.storage.StorageKey
import com.slimly.core.storage.value

/**
 * Ключ подмены отказа. Виден и конвейеру: он читает подмену из хранилища,
 * а не из экранного состояния меню — см. [SimulatedFailureProvider].
 */
val simulatedFailureKey = StorageKey.ofEnum<SimulatedFailure>("slimly.debug.simulatedFailure")

/**
 * Отказ, который отладочное меню подставляет вместо настоящей обработки.
 *
 * Экран итога умеет показывать список отказов, но дождаться настоящего сбоя
 * от системы почти невозможно: место кончается редко, из облака файл
 * докачивается быстро. Подмена — единственный способ увидеть этот список
 * глазами.
 */
enum class SimulatedFailure(
    @get:StringRes val titleRes: Int,
    /**
     * Причина отказа. Текст попадает в список итога рядом с файлом, поэтому
     * написан для пользователя, а не для лога. `null` — подмены нет.
     */
    @get:StringRes val reasonRes: Int?,
) {
    OFF(R.string.failure_none, null),
    NO_SPACE(R.string.failure_no_space, R.string.failure_detail_no_space),
    UNREADABLE(R.string.failure_unreadable, R.string.failure_detail_unreadable),
    INTERRUPTED(R.string.failure_interrupted, R.string.failure_detail_interrupted),
    SAVE_DENIED(R.string.failure_save_denied, R.string.failure_detail_save_denied),
}

/**
 * Состояние отладочного меню: что именно сборка сейчас подменяет.
 *
 * Переживает перезапуск, потому что проверяют обычно не один экран, а путь
 * до него: перещёлкивать тумблер после каждого запуска — верный способ
 * проверить не то.
 *
 * Состояние читается с экрана, поэтому лежит в `mutableStateOf`, а хранилище
 * доезжает до него приостанавливаемым [load]: синхронного чтения у DataStore
 * нет.
 */
class DebugSettings(private val storage: KeyValueStorage) {
    var simulatedFailure: SimulatedFailure by mutableStateOf(SimulatedFailure.OFF)
        private set

    /**
     * Подменённые значения удалённого конфига. Здесь они лежат ради экрана:
     * ему нужно и показать подмену рядом с настоящим значением, и знать,
     * что она вообще есть.
     */
    var remoteConfigOverrides: RemoteConfigOverrides by mutableStateOf(RemoteConfigOverrides())
        private set

    /**
     * Хоть одна подмена включена. Пока это так, поверх экранов горит метка
     * DEBUG: странное поведение сборки должно объясняться раньше, чем его
     * пойдут искать в коде.
     */
    val isOverridden: Boolean
        get() = simulatedFailure != SimulatedFailure.OFF || !remoteConfigOverrides.isEmpty

    suspend fun load() {
        simulatedFailure = storage.value(simulatedFailureKey, SimulatedFailure.OFF)
        remoteConfigOverrides = storage.value(remoteConfigOverridesKey, RemoteConfigOverrides())
    }

    suspend fun setSimulatedFailure(failure: SimulatedFailure) {
        simulatedFailure = failure
        storage.set(simulatedFailureKey, failure)
    }

    /**
     * Пустой текст — это не значение, а «подмены нет»: разбор пустой строки
     * всё равно провалился бы, и ключ получил бы своё умолчание вместо того,
     * что пришло с сервера.
     */
    suspend fun setRemoteConfigOverride(text: String?, name: String) {
        val trimmed = text?.trim()?.takeIf { it.isNotEmpty() }
        remoteConfigOverrides = remoteConfigOverrides.with(name, trimmed)
        storage.set(remoteConfigOverridesKey, remoteConfigOverrides)
    }

    suspend fun clearRemoteConfigOverrides() {
        remoteConfigOverrides = RemoteConfigOverrides()
        storage.set(remoteConfigOverridesKey, remoteConfigOverrides)
    }

    suspend fun reset() {
        setSimulatedFailure(SimulatedFailure.OFF)
        clearRemoteConfigOverrides()
    }
}
