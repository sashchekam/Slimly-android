package com.slimly.core.format

import android.content.res.Resources
import android.icu.text.DateFormat
import android.icu.text.DateIntervalFormat
import com.slimly.core.R
import com.slimly.core.model.ProcessingDigest
import com.slimly.core.model.StatsRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import android.icu.util.DateInterval as IcuDateInterval

interface MediaDateFormatting {
    /** «Сегодня», «5 мая», «2–5 мая 2025», «28 декабря 2024 — 3 января 2025». */
    fun string(earliest: Instant, latest: Instant, now: Instant = Instant.now()): String

    /** Одна дата числом и месяцем: «12 января», «12 января 2024». */
    fun day(date: Instant, now: Instant = Instant.now()): String

    /** Подпись периода: «1 июня — 4 сентября · 90 дней», «с 12 января · всё время». */
    fun periodText(digest: ProcessingDigest, now: Instant = Instant.now()): String
}

/**
 * Даты медиатеки словами.
 *
 * Промежуток пишется как в системной галерее: общая часть не повторяется —
 * «2–5 мая 2025» вместо «2 мая 2025 — 5 мая 2025». Год текущего года
 * опускаем: он ничего не уточняет, а места над сеткой мало.
 *
 * Сокращает промежуток система: где именно опустить общий месяц и как
 * развернуть тире, в каждом языке решено по-своему.
 */
class MediaDateFormatter(private val resources: Resources, private val zone: ZoneId = ZoneId.systemDefault()) :
    MediaDateFormatting {
    override fun string(earliest: Instant, latest: Instant, now: Instant): String {
        val start = minOf(earliest, latest)
        val end = maxOf(earliest, latest)

        if (date(start) == date(end)) {
            // День назван словом, только когда он вообще подписан словом:
            // «Сегодня» рядом с числом соседнего дня выглядело бы разнобоем.
            val today = date(now)
            return when (date(end)) {
                today -> resources.getString(R.string.date_today)
                today.minusDays(1) -> resources.getString(R.string.date_yesterday)
                else -> day(end, now)
            }
        }

        val showsYear = !isCurrentYear(start, now) || !isCurrentYear(end, now)
        val skeleton = if (showsYear) DateFormat.YEAR_MONTH_DAY else DateFormat.MONTH_DAY
        return DateIntervalFormat.getInstance(skeleton, resources.locale)
            .format(IcuDateInterval(start.toEpochMilli(), end.toEpochMilli()))
    }

    /**
     * Без «сегодня» и «вчера»: строка идёт внутри фразы («с 12 января»),
     * где относительное слово теряет падеж.
     */
    override fun day(date: Instant, now: Instant): String {
        val skeleton = if (isCurrentYear(date, now)) DateFormat.MONTH_DAY else DateFormat.YEAR_MONTH_DAY
        return DateFormat.getInstanceForSkeleton(skeleton, resources.locale)
            .format(date.toEpochMilli())
    }

    override fun periodText(digest: ProcessingDigest, now: Instant): String {
        val interval = digest.interval ?: return resources.getString(R.string.digest_empty)
        val caption = resources.getString(digest.range.captionRes)

        if (digest.range == StatsRange.ALL_TIME) {
            return resources.getString(R.string.digest_since, day(interval.start, now), caption)
        }
        return "${string(interval.start, interval.end, now)} · $caption"
    }

    private fun date(moment: Instant): LocalDate = LocalDate.ofInstant(moment, zone)

    private fun isCurrentYear(moment: Instant, now: Instant): Boolean = date(moment).year == date(now).year
}
