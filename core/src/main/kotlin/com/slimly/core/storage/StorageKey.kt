package com.slimly.core.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Ключ значения: имя в хранилище плюс способ превратить значение в примитив.
 *
 * Тип держит ключ, а не место вызова: положить по `onboardingTourSeen` строку
 * или прочитать оттуда число не выйдет — не соберётся.
 *
 * Объявляется ключ рядом с тем, кто им владеет:
 * ```kotlin
 * internal val onboardingTourSeenKey = StorageKey.flag("slimly.onboarding.tourSeen")
 * ```
 */
class StorageKey<T : Any>(val name: String, val codec: StorageCodec<T>) {
    companion object {
        fun flag(name: String): StorageKey<Boolean> = StorageKey(name, FlagCodec())

        fun whole(name: String): StorageKey<Int> = StorageKey(name, WholeCodec())

        fun decimal(name: String): StorageKey<Double> = StorageKey(name, DecimalCodec())

        fun text(name: String): StorageKey<String> = StorageKey(name, TextCodec())

        /**
         * Вариант перечисления лежит своим именем: числовой порядок сместился
         * бы от вставки нового варианта в середину списка.
         */
        inline fun <reified E : Enum<E>> ofEnum(name: String): StorageKey<E> =
            StorageKey(name, EnumCodec(enumValues<E>().toList()))

        /** Значение без примитивного вида — оно уезжает строкой JSON. */
        fun <T : Any> json(name: String, serializer: KSerializer<T>): StorageKey<T> =
            StorageKey(name, JsonCodec(serializer))
    }
}

internal class FlagCodec : StorageCodec<Boolean> {
    override fun decode(stored: StoredValue): Boolean? = (stored as? StoredValue.Flag)?.value

    override fun encode(value: Boolean): StoredValue = StoredValue.Flag(value)
}

internal class WholeCodec : StorageCodec<Int> {
    override fun decode(stored: StoredValue): Int? = (stored as? StoredValue.Whole)?.value

    override fun encode(value: Int): StoredValue = StoredValue.Whole(value)
}

internal class DecimalCodec : StorageCodec<Double> {
    override fun decode(stored: StoredValue): Double? = (stored as? StoredValue.Decimal)?.value

    override fun encode(value: Double): StoredValue = StoredValue.Decimal(value)
}

internal class TextCodec : StorageCodec<String> {
    override fun decode(stored: StoredValue): String? = (stored as? StoredValue.Text)?.value

    override fun encode(value: String): StoredValue = StoredValue.Text(value)
}

/**
 * Публичный, а не `internal`, потому что его создаёт встраиваемая
 * [StorageKey.ofEnum] — она разворачивается на месте вызова.
 */
class EnumCodec<E : Enum<E>>(private val entries: List<E>) : StorageCodec<E> {
    override fun decode(stored: StoredValue): E? {
        val name = (stored as? StoredValue.Text)?.value ?: return null
        return entries.firstOrNull { it.name == name }
    }

    override fun encode(value: E): StoredValue = StoredValue.Text(value.name)
}

internal class JsonCodec<T : Any>(private val serializer: KSerializer<T>) : StorageCodec<T> {
    /**
     * Разбор не удался — значит, значения нет: в хранилище могла остаться
     * запись от прошлой версии модели, и падать из-за неё приложение
     * не должно.
     */
    override fun decode(stored: StoredValue): T? {
        val text = (stored as? StoredValue.Text)?.value ?: return null
        return runCatching { format.decodeFromString(serializer, text) }.getOrNull()
    }

    override fun encode(value: T): StoredValue = StoredValue.Text(format.encodeToString(serializer, value))

    private companion object {
        /** Неизвестные поля пропускаем: запись от новой версии не должна ронять старую. */
        val format = Json { ignoreUnknownKeys = true }
    }
}
