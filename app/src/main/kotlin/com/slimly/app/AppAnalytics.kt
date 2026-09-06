package com.slimly.app

import com.slimly.core.analytics.AnalyticsService
import com.slimly.core.analytics.AnalyticsValue
import com.slimly.core.analytics.FeatureAnalytics
import com.slimly.core.deeplink.DeeplinkUrl

/**
 * Аналитика самого приложения: то, что случается вне экранов.
 *
 * Первый пример модульной аналитики — дальше такая же пара «префикс плюс
 * словарь событий» заводится в каждой фиче и получает общую аналитику
 * параметром конструктора.
 */
internal class AppAnalytics(override val analytics: AnalyticsService) : FeatureAnalytics {
    override val scope: String = "app"

    /**
     * Ссылка привела на экран. Адрес целиком не отправляем: в нём бывают
     * параметры пользователя, а для отчёта хватает точки входа и того,
     * откуда по ней пришли.
     */
    fun didOpenDeeplink(deeplink: DeeplinkUrl, source: DeeplinkSource) {
        track(
            name = "deeplink_opened",
            parameters = mapOf(
                "target" to AnalyticsValue.of(deeplink.host),
                "source" to AnalyticsValue.of(source.value),
            ),
        )
    }

    /**
     * Ссылка есть, а маршрута под неё нет: обычно это ярлык, оставшийся
     * от прошлой версии, — такое видно только по аналитике.
     */
    fun didFailDeeplink(deeplink: DeeplinkUrl) {
        track(name = "deeplink_unknown", parameters = mapOf("target" to AnalyticsValue.of(deeplink.host)))
    }
}

/**
 * Откуда пришла ссылка.
 *
 * Виджет, ссылка из заметки и переход из браузера приходят одинаково —
 * системным открытием адреса, и разделить их всё равно нечем. А вот ярлык
 * иконки приходит своим действием, и по этой границе видно, пользуются ли
 * быстрыми действиями вообще.
 */
internal enum class DeeplinkSource(val value: String) {
    URL("url"),
    SHORTCUT("shortcut"),
}
