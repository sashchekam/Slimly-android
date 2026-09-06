package com.slimly.core.remoteconfig

/**
 * Разбор значения, приехавшего строкой.
 *
 * Строкой его отдаёт и Firebase, и любой другой источник, и такой же строкой
 * оно набирается руками в веб-консоли. `null` — «значения нет»: владелец
 * ключа получит своё умолчание.
 */
fun interface RemoteConfigParser<T : Any> {
    fun parse(text: String): T?
}

/**
 * Ключ удалённого конфига вместе со значением по умолчанию.
 *
 * Умолчание лежит рядом с именем, а не на месте чтения: до первой удачной
 * загрузки, без сети и без Firebase значение всё равно нужно, и оно должно
 * быть одно на всё приложение.
 */
class RemoteConfigKey<T : Any>(val name: String, val defaultValue: T, val parser: RemoteConfigParser<T>) {
    companion object {
        fun text(name: String, default: String): RemoteConfigKey<String> = RemoteConfigKey(name, default) { it }

        fun whole(name: String, default: Int): RemoteConfigKey<Int> =
            RemoteConfigKey(name, default) { it.toIntOrNull() }

        fun decimal(name: String, default: Double): RemoteConfigKey<Double> =
            RemoteConfigKey(name, default) { it.toDoubleOrNull() }

        /**
         * В веб-консоли флаг набирают по-разному — «true», «1», «yes».
         * Ошибка в написании не должна тихо превращаться в противоположный
         * флаг, поэтому всё непонятное — это «значения нет».
         */
        fun flag(name: String, default: Boolean): RemoteConfigKey<Boolean> = RemoteConfigKey(name, default) { text ->
            when (text.lowercase()) {
                "true", "yes", "1" -> true
                "false", "no", "0" -> false
                else -> null
            }
        }
    }
}
