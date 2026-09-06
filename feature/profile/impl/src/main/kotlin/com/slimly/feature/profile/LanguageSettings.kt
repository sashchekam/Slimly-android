package com.slimly.feature.profile

import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Язык приложения — системная настройка, а не наша.
 *
 * Иосный обход с подменой каталога сюда не переехал намеренно: на Android
 * язык отдельного приложения выбирают в системных настройках, а список
 * доступных берётся из `locales_config.xml`. Нам остаётся показать текущий
 * язык и открыть тот самый экран.
 *
 * До Android 13 такого экрана нет вовсе, и выбирать не из чего: приложение
 * говорит на языке телефона.
 */
class LanguageSettings(private val sdk: Int = Build.VERSION.SDK_INT) {
    val isAvailable: Boolean
        get() = sdk >= Build.VERSION_CODES.TIRAMISU

    /**
     * Язык на своём языке: «Deutsch», «Русский». Не на языке интерфейса —
     * строка стоит там, где выбирают язык, и «Немецкий» помог бы только тому,
     * кто уже читает по-русски.
     */
    fun currentName(resources: Resources): String {
        val locale = resources.configuration.locales[0]
        val name = locale.getDisplayLanguage(locale)
        return name.replaceFirstChar { it.titlecase(locale) }
    }

    /**
     * Настройки языка этого приложения. Открывает их экран: системный экран
     * показывается поверх Activity, а не поверх сервиса.
     */
    fun intent(packageName: String): Intent =
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", packageName, null))
}
