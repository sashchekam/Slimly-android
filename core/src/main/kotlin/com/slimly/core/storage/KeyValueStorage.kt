package com.slimly.core.storage

import kotlinx.coroutines.flow.Flow

/**
 * Хранилище значений по ключам.
 *
 * Владелец значения знает только ключ и тип: где именно лежит отметка —
 * в DataStore, в памяти теста или в связке ключей — решает сборка
 * приложения, подставив другую реализацию.
 *
 * Чтение приостанавливаемое, а не мгновенное, как `UserDefaults` на iOS:
 * у DataStore синхронного доступа нет вовсе, и обходить это блокировкой
 * потока значило бы возвращать ровно ту проблему, ради которой его завели.
 */
interface KeyValueStorage {
    /** Значение и все его последующие изменения. */
    fun <T : Any> observe(key: StorageKey<T>): Flow<T?>

    suspend fun <T : Any> value(key: StorageKey<T>): T?

    suspend fun <T : Any> set(key: StorageKey<T>, value: T)

    suspend fun <T : Any> remove(key: StorageKey<T>)
}

/**
 * Чтение со значением по умолчанию: «ключа нет» и «лежит false» для
 * владельца — одно и то же, разделять их приходится редко.
 */
suspend fun <T : Any> KeyValueStorage.value(key: StorageKey<T>, default: T): T = value(key) ?: default
