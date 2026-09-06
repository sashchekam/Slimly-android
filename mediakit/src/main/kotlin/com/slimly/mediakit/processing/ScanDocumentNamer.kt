package com.slimly.mediakit.processing

import android.content.res.Resources
import com.slimly.mediakit.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Имя собранного документа. */
internal interface DocumentNaming {
    fun name(at: Instant = Instant.now()): String
}

/**
 * «Скан 2026-09-06 14-23.pdf».
 *
 * Дата в имени, потому что своего имени у документа нет: страницы приходят
 * из медиатеки под номерами камеры, а сканов в папке со временем копится
 * много.
 */
internal class ScanDocumentNamer(private val resources: Resources) : DocumentNaming {
    /**
     * Формат отметки фиксированный, поэтому и локаль фиксированная: с чужим
     * календарём «2026» превратилось бы в год другой эры, а имена файлов
     * должны сортироваться одинаково у всех. Двоеточие в имени файла
     * системе не нравится — время разделяем дефисом.
     */
    private val stamp = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH-mm", Locale.US)
        .withZone(ZoneId.systemDefault())

    override fun name(at: Instant): String = resources.getString(R.string.scan_file_name, stamp.format(at))
}
