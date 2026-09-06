package com.slimly.core.remoteconfig

/**
 * Удалённый конфиг: значения, которыми можно управлять без выпуска сборки.
 *
 * Реализация отвечает только за сырую строку по имени — разбор и умолчание
 * одинаковы для всех источников и живут в расширении ниже.
 */
interface RemoteConfigService {
    /**
     * Имена всех значений, которые есть у источника. Нужны отладочному меню:
     * список ключей знает только источник, а показать оно должно то, что
     * реально приехало, а не то, что приложение догадалось спросить.
     */
    val names: List<String>

    /** `null` — значения нет, владелец ключа получит своё умолчание. */
    fun text(name: String): String?

    /**
     * Забрать свежие значения. Молча: сорвавшаяся загрузка — это не ошибка
     * экрана, а повод показать то, что уже есть.
     */
    suspend fun refresh()
}

fun <T : Any> RemoteConfigService.value(key: RemoteConfigKey<T>): T =
    text(key.name)?.let(key.parser::parse) ?: key.defaultValue

/**
 * Конфиг без источника: отдаёт то, что положили руками, а всё остальное —
 * умолчаниями ключей.
 *
 * На нём работают превью, тесты и сборка без `google-services.json`.
 */
class LocalRemoteConfig(private val values: Map<String, String> = emptyMap()) : RemoteConfigService {
    override val names: List<String>
        get() = values.keys.sorted()

    override fun text(name: String): String? = values[name]

    override suspend fun refresh() = Unit
}
