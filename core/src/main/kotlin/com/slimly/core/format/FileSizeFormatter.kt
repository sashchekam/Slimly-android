package com.slimly.core.format

import android.content.res.Resources
import com.slimly.core.R
import java.text.NumberFormat

/** Число и единица по отдельности: в профиле единица набирается мельче цифры. */
data class FileSizeParts(val value: String, val unit: String)

enum class FileSizeUnit {
    AUTOMATIC,
    MEGABYTES,
    GIGABYTES,
}

interface FileSizeFormatting {
    fun string(bytes: Long, unit: FileSizeUnit = FileSizeUnit.AUTOMATIC): String

    fun parts(bytes: Long, unit: FileSizeUnit = FileSizeUnit.AUTOMATIC): FileSizeParts
}

/**
 * Размеры файлов словами.
 *
 * Точность падает вместе с ростом числа: гигабайты и мелкие мегабайты
 * показываем с десятой, крупные мегабайты округляем целиком — на глаз
 * «186,4 МБ» не полезнее «186 МБ», а строка длиннее. Разделитель дробной
 * части и сокращение единицы берём из локали: «186,4 МБ» по-русски
 * и «186.4 MB» по-английски.
 */
class FileSizeFormatter(private val resources: Resources) : FileSizeFormatting {
    override fun string(bytes: Long, unit: FileSizeUnit): String {
        val parts = parts(bytes, unit)
        return "${parts.value} ${parts.unit}"
    }

    override fun parts(bytes: Long, unit: FileSizeUnit): FileSizeParts {
        val megabytes = bytes / BYTES_IN_MEGABYTE
        val usesGigabytes = when (unit) {
            FileSizeUnit.GIGABYTES -> true
            FileSizeUnit.MEGABYTES -> false
            FileSizeUnit.AUTOMATIC -> megabytes >= MEGABYTES_IN_GIGABYTE
        }

        return if (usesGigabytes) {
            FileSizeParts(
                value = number(megabytes / MEGABYTES_IN_GIGABYTE, fraction = 1),
                unit = resources.getString(R.string.size_gigabyte),
            )
        } else {
            FileSizeParts(
                value = number(megabytes, fraction = if (megabytes >= WHOLE_FROM) 0 else 1),
                unit = resources.getString(R.string.size_megabyte),
            )
        }
    }

    private fun number(value: Double, fraction: Int): String = NumberFormat.getNumberInstance(resources.locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = fraction
    }.format(value)

    private companion object {
        /**
         * Десятичные мегабайты, а не двоичные: так считают и системная
         * галерея, и настройки. С делением на 1024² тот же файл выглядел бы
         * на 5% легче, чем в галерее, — и пользователь решил бы, что мы врём.
         */
        const val BYTES_IN_MEGABYTE = 1_000_000.0
        const val MEGABYTES_IN_GIGABYTE = 1000.0

        /** Отсюда десятая доля уже не уточняет, а удлиняет строку. */
        const val WHOLE_FROM = 100
    }
}
