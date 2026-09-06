package com.slimly.core.analytics

/**
 * Событие аналитики: имя и параметры.
 *
 * Имя здесь уже готовое — с префиксом модуля и в змеином регистре. Собирает
 * его аналитика модуля ([FeatureAnalytics]), а не место вызова.
 */
data class AnalyticsEvent(val name: String, val parameters: Map<String, AnalyticsValue> = emptyMap())

/**
 * Свойство пользователя — срез, по которому потом режется отчёт
 * и таргетируется удалённый конфиг.
 */
data class AnalyticsUserProperty(val name: String)

/**
 * Значение параметра события.
 *
 * Перечисление, а не `Any`: событие уходит в аналитику из любого места
 * приложения, а системы аналитики принимают лишь несколько типов — лучше
 * не пропустить чужой на компиляции, чем терять параметр молча в рантайме.
 */
sealed interface AnalyticsValue {
    /**
     * Значение строкой — для лога и отладочных экранов. Наружу параметр
     * уходит своим типом: приведение к строке — дело показа, а не отправки.
     */
    val text: String

    data class Text(val value: String) : AnalyticsValue {
        override val text: String get() = value
    }

    data class Whole(val value: Int) : AnalyticsValue {
        override val text: String get() = value.toString()
    }

    data class Decimal(val value: Double) : AnalyticsValue {
        override val text: String get() = value.toString()
    }

    data class Flag(val value: Boolean) : AnalyticsValue {
        override val text: String get() = value.toString()
    }

    companion object {
        fun of(value: String): AnalyticsValue = Text(value)

        fun of(value: Int): AnalyticsValue = Whole(value)

        fun of(value: Double): AnalyticsValue = Decimal(value)

        fun of(value: Boolean): AnalyticsValue = Flag(value)
    }
}
