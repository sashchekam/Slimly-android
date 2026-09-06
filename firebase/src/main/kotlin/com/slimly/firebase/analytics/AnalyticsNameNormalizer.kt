package com.slimly.firebase.analytics

import java.util.Locale

/** Привести имя события или свойства к тому, что принимает Firebase. */
internal fun interface AnalyticsNameNormalizing {
    fun normalized(name: String): String
}

/**
 * Firebase молча выбрасывает событие с недопустимым именем: только латиница,
 * цифры и подчёркивания, не длиннее сорока символов, первый символ — буква.
 * Проверять это глазами по всем модулям не выйдет, поэтому имя чинится
 * на выходе.
 */
internal class AnalyticsNameNormalizer : AnalyticsNameNormalizing {
    override fun normalized(name: String): String {
        val cleaned = name.lowercase(Locale.ROOT).map(::allowed)
        // Имя, начавшееся с цифры или подчёркивания, Firebase не примет,
        // а срезать первый символ — значит потерять смысл имени.
        val prefixed = if (cleaned.firstOrNull()?.isLetter() == true) cleaned else listOf(SEPARATOR) + cleaned
        return prefixed.take(LIMIT).joinToString(separator = "")
    }

    private fun allowed(character: Char): Char =
        if (character.code < ASCII_LIMIT && character.isLetterOrDigit()) character else SEPARATOR

    private companion object {
        const val LIMIT = 40
        const val SEPARATOR = '_'
        const val ASCII_LIMIT = 128
    }
}
