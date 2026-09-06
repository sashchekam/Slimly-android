package com.slimly.core.storage

/**
 * Примитив, которым хранилище оперирует на самом деле.
 *
 * Дальше этих четырёх случаев не идём намеренно: всё остальное либо
 * раскладывается по ним, либо уезжает строкой JSON — иначе хранилище
 * пришлось бы менять под каждый новый тип значения.
 */
sealed interface StoredValue {
    data class Flag(val value: Boolean) : StoredValue

    data class Whole(val value: Int) : StoredValue

    data class Decimal(val value: Double) : StoredValue

    data class Text(val value: String) : StoredValue
}

/**
 * Превращение значения в примитив хранилища и обратно.
 *
 * Живёт на ключе, а не на хранилище: так новый вид значения не трогает
 * ни один бэкенд, а место вызова получает готовый тип.
 */
interface StorageCodec<T : Any> {
    /** `null` — по имени лежит что-то другое: значения для владельца ключа нет. */
    fun decode(stored: StoredValue): T?

    fun encode(value: T): StoredValue
}
