package com.slimly.firebase.analytics

import android.os.Bundle
import com.slimly.core.analytics.AnalyticsValue

/** Разложить параметры события в связку, которую понимает SDK. */
internal fun interface AnalyticsParameterEncoding {
    fun encoded(parameters: Map<String, AnalyticsValue>): Bundle?
}

/**
 * Firebase принимает только строку, целое и дробное: флаг едет единицей
 * и нулём, длинная строка обрезается — иначе параметр отбрасывается целиком
 * вместе со своим значением.
 */
internal class AnalyticsParameterEncoder(private val names: AnalyticsNameNormalizing = AnalyticsNameNormalizer()) :
    AnalyticsParameterEncoding {
    override fun encoded(parameters: Map<String, AnalyticsValue>): Bundle? {
        if (parameters.isEmpty()) return null

        return Bundle(parameters.size).apply {
            parameters.forEach { (name, value) -> put(names.normalized(name), value) }
        }
    }

    private fun Bundle.put(name: String, value: AnalyticsValue) {
        when (value) {
            is AnalyticsValue.Text -> putString(name, value.value.take(TEXT_LIMIT))
            is AnalyticsValue.Whole -> putInt(name, value.value)
            is AnalyticsValue.Decimal -> putDouble(name, value.value)
            is AnalyticsValue.Flag -> putInt(name, if (value.value) TRUE else FALSE)
        }
    }

    private companion object {
        const val TEXT_LIMIT = 100
        const val TRUE = 1
        const val FALSE = 0
    }
}
