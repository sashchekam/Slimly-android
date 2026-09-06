package com.slimly.core.model

import androidx.annotation.StringRes
import com.slimly.core.R
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Глубина среза статистики.
 *
 * Три положения, а не свободный выбор дат: у приложения, которым пользуются
 * набегами, «последний месяц» и «всё время» — разные истории, а календарь
 * между ними ничего не уточняет.
 */
enum class StatsRange(
    /**
     * Глубина в днях. `null` — у «всего времени» начало не отсчитывается
     * назад, а берётся из первой записи журнала.
     */
    val days: Int?,
    @get:StringRes val titleRes: Int,
    /** Хвост подписи периода: «… · 30 дней», «… · всё время». */
    @get:StringRes val captionRes: Int,
) {
    MONTH(30, R.string.period_month, R.string.period_month),
    QUARTER(90, R.string.period_quarter, R.string.period_quarter),
    ALL_TIME(null, R.string.period_all_time, R.string.period_all_time_inline),
}

/** Отрезок времени, за который посчитан срез. */
data class DateInterval(val start: Instant, val end: Instant) {
    val durationMillis: Long
        get() = end.toEpochMilli() - start.toEpochMilli()

    operator fun contains(moment: Instant): Boolean = moment >= start && moment <= end
}

/**
 * Журнал сделанного — то, из чего собирается статистика по инструментам.
 *
 * Лежит рядом с общей суммой, а не вместо неё: сумма считается с первого
 * запуска и не должна меняться от того, что журнал подрезали по краю.
 */
@Serializable
data class ProcessingHistory(
    /** От старых к свежим — в этом же порядке их читает искра. */
    val records: List<ProcessingRecord> = emptyList(),
) {
    val isEmpty: Boolean
        get() = records.isEmpty()

    /**
     * Лишнее срезается с головы: свежие записи нужнее старых, а «всё время»
     * после подрезки честно начинается с первой уцелевшей.
     */
    fun appending(record: ProcessingRecord): ProcessingHistory = ProcessingHistory((records + record).takeLast(LIMIT))

    fun digest(range: StatsRange, now: Instant = Instant.now()): ProcessingDigest =
        ProcessingDigest.of(records = records, range = range, now = now)

    companion object {
        /**
         * Сколько записей храним. Запись — это один запуск обработки, а не
         * файл, поэтому тысячи хватает надолго. Предел нужен потому, что
         * журнал лежит в настройках целиком, одним значением.
         */
        const val LIMIT = 1000

        val EMPTY = ProcessingHistory()
    }
}
