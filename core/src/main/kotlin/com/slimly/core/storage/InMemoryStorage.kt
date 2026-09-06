package com.slimly.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Хранилище в памяти: превью и тесты, которым нужно чистое состояние
 * на каждый запуск и никаких следов на устройстве.
 */
class InMemoryStorage(values: Map<String, StoredValue> = emptyMap()) : KeyValueStorage {
    private val state = MutableStateFlow(values)

    override fun <T : Any> observe(key: StorageKey<T>): Flow<T?> = state.map { it.decode(key) }.distinctUntilChanged()

    override suspend fun <T : Any> value(key: StorageKey<T>): T? = state.first().decode(key)

    override suspend fun <T : Any> set(key: StorageKey<T>, value: T) {
        state.update { it + (key.name to key.codec.encode(value)) }
    }

    override suspend fun <T : Any> remove(key: StorageKey<T>) {
        state.update { it - key.name }
    }

    private fun <T : Any> Map<String, StoredValue>.decode(key: StorageKey<T>): T? =
        this[key.name]?.let(key.codec::decode)
}
