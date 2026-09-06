package com.slimly.core.format

import androidx.annotation.PluralsRes
import com.slimly.core.R

/**
 * Что именно считаем. От этого зависит не только слово, но и набор форм:
 * «фото» не склоняется, а «файл» склоняется, и знает об этом каталог строк.
 *
 * Формы подбирает система по количеству, поэтому в коде нет ни одной таблицы
 * склонений — только ссылка на набор вариантов.
 */
enum class CountNoun(
    /** «3 файла» — число внутри фразы. */
    @get:PluralsRes val countRes: Int,
    /** «файла» — одно слово: в профиле цифра набирается крупно, а подпись мелко. */
    @get:PluralsRes val wordRes: Int,
) {
    FILE(R.plurals.count_file, R.plurals.unit_file),
    PHOTO(R.plurals.count_photo, R.plurals.unit_photo),
    VIDEO(R.plurals.count_video, R.plurals.unit_video),
    PAGE(R.plurals.count_page, R.plurals.unit_page),
    TRACK(R.plurals.count_track, R.plurals.unit_track),
    CONVERSION(R.plurals.count_conversion, R.plurals.unit_conversion),
    SCAN(R.plurals.count_scan, R.plurals.unit_scan),
}
