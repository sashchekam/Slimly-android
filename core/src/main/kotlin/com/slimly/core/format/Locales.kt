package com.slimly.core.format

import android.content.res.Resources
import java.util.Locale

/**
 * Язык, на котором приложение сейчас говорит.
 *
 * Берём его у ресурсов, а не у `Locale.getDefault()`: язык приложения
 * ставится системной настройкой отдельно от языка телефона, и форматы
 * должны идти за первым.
 */
internal val Resources.locale: Locale
    get() = configuration.locales[0]
