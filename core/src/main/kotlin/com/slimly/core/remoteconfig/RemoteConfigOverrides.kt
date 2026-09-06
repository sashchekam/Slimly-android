package com.slimly.core.remoteconfig

import com.slimly.core.storage.KeyValueStorage
import com.slimly.core.storage.StorageKey
import com.slimly.core.storage.value
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/**
 * Локальные подмены удалённого конфига: имя ключа → текст значения.
 *
 * Подмена живёт только на этом устройстве и никуда не уезжает: это способ
 * посмотреть на поведение флага у себя, не трогая веб-консоль и остальных.
 * Значение здесь строкой, как и в самом конфиге, — разбирать его будет
 * владелец ключа, а не хранилище подмен.
 */
@Serializable
data class RemoteConfigOverrides(private val values: Map<String, String> = emptyMap()) {
    val names: List<String>
        get() = values.keys.sorted()

    val isEmpty: Boolean
        get() = values.isEmpty()

    operator fun get(name: String): String? = values[name]

    fun with(name: String, text: String?): RemoteConfigOverrides =
        RemoteConfigOverrides(if (text == null) values - name else values + (name to text))
}

val remoteConfigOverridesKey: StorageKey<RemoteConfigOverrides> =
    StorageKey.json("slimly.debug.remoteConfigOverrides", serializer())

/**
 * Источник конфига, поверх которого лежат локальные подмены.
 *
 * Собирается только в отладочной сборке: в релизе подменять значения нечем,
 * и приложение читает источник напрямую.
 *
 * Подмены держим полем, а не читаем из хранилища на каждом обращении:
 * у DataStore синхронного чтения нет, а значение спрашивают откуда угодно
 * и без ожидания. Поле обновляет поток из того же хранилища.
 */
class OverridingRemoteConfig(private val base: RemoteConfigService, private val storage: KeyValueStorage) :
    RemoteConfigService {
    @Volatile
    private var overrides = RemoteConfigOverrides()

    /**
     * Подменить можно и то, чего в источнике нет: ключ мог ещё не доехать
     * до веб-консоли, а проверить поведение нужно уже сейчас.
     */
    override val names: List<String>
        get() = (base.names + overrides.names).distinct().sorted()

    override fun text(name: String): String? = overrides[name] ?: base.text(name)

    override suspend fun refresh() {
        overrides = storage.value(remoteConfigOverridesKey, RemoteConfigOverrides())
        base.refresh()
    }

    /** Подмены, меняющиеся из отладочного меню, доезжают сюда потоком. */
    fun apply(overrides: RemoteConfigOverrides) {
        this.overrides = overrides
    }
}
