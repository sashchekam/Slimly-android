package com.slimly.core.analytics

/**
 * Общая аналитика приложения — единственная дверь наружу.
 *
 * Кто именно за ней стоит (Firebase, лог, заглушка в тесте), знает только
 * сборка приложения. Модуль держит [AnalyticsService] и не тянет за собой
 * ни одного SDK.
 */
interface AnalyticsService {
    fun track(event: AnalyticsEvent)

    /**
     * Свойство живёт до переустановки и приезжает со всеми следующими
     * событиями. `null` — снять.
     */
    fun set(property: AnalyticsUserProperty, value: String?)
}

/**
 * Аналитика, которая никуда не отправляет.
 *
 * Нужна не только тестам: без `google-services.json` приложение всё равно
 * должно запускаться и работать, поэтому событиям нужен приёмник,
 * который молчит.
 */
class NoAnalytics : AnalyticsService {
    override fun track(event: AnalyticsEvent) = Unit

    override fun set(property: AnalyticsUserProperty, value: String?) = Unit
}

/**
 * Несколько приёмников за одним интерфейсом.
 *
 * Отладочная сборка пишет событие и в Firebase, и в лог: увидеть, что именно
 * ушло, иначе можно только в веб-консоли и через сутки.
 */
class CompositeAnalytics(private val services: List<AnalyticsService>) : AnalyticsService {
    override fun track(event: AnalyticsEvent) {
        services.forEach { it.track(event) }
    }

    override fun set(property: AnalyticsUserProperty, value: String?) {
        services.forEach { it.set(property, value) }
    }
}

/**
 * База для аналитики модуля.
 *
 * Модуль не отправляет события сам: он получает общую аналитику приложения
 * параметром конструктора и складывает поверх неё свой словарь событий —
 * со своими именами, своими параметрами и своим префиксом. Тогда все имена
 * модуля видны в одном файле, а место вызова пишет `analytics.didPickFiles(3)`
 * вместо строкового литерала.
 *
 * ```kotlin
 * class CompressAnalytics(override val analytics: AnalyticsService) : FeatureAnalytics {
 *     override val scope = "compress"
 *
 *     fun didPickFiles(count: Int) = track("files_picked", mapOf("count" to AnalyticsValue.of(count)))
 * }
 * ```
 */
interface FeatureAnalytics {
    /**
     * Префикс имён событий модуля — «compress», «scan», «onboarding».
     * По нему события модуля отбираются в отчёте одной маской.
     */
    val scope: String

    val analytics: AnalyticsService

    fun track(name: String, parameters: Map<String, AnalyticsValue> = emptyMap()) {
        analytics.track(AnalyticsEvent("${scope}_$name", parameters))
    }
}
