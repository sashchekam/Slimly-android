package com.slimly.core.format

import android.content.res.Resources
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.time.Duration

interface DurationFormatting {
    /** «0:42», «2:15», «1:02:15». */
    fun string(duration: Duration): String

    /** Суммарная длительность: «46 мин», «2 ч 14 мин». */
    fun total(seconds: Double): String

    /** «12 с», «3 мин» — грубая оценка, поэтому без долей. */
    fun remaining(duration: Duration): String
}

/**
 * Длительности словами.
 *
 * Двоеточие между минутами и секундами одинаково во всех языках, которые мы
 * поддерживаем, поэтому позиционная строка собирается вручную. А вот единицы
 * подписывает система: их сокращения в каждом языке свои, а вместе с ними
 * и порядок слов.
 */
class DurationFormatter(private val resources: Resources) : DurationFormatting {
    override fun string(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val seconds = totalSeconds % SECONDS_IN_MINUTE
        val minutes = (totalSeconds / SECONDS_IN_MINUTE) % MINUTES_IN_HOUR
        val hours = totalSeconds / SECONDS_IN_HOUR

        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Секунд здесь нет: это итог за месяцы, и точность до секунды в нём была
     * бы шумом.
     */
    override fun total(seconds: Double): String {
        val minutes = maxOf(0L, (seconds / SECONDS_IN_MINUTE).roundToLong())
        if (minutes < MINUTES_IN_HOUR) return measures(minute(minutes))

        return measures(
            Measure(minutes / MINUTES_IN_HOUR, MeasureUnit.HOUR),
            minute(minutes % MINUTES_IN_HOUR),
        )
    }

    /** Без секунд после минуты: точность, которой у прогноза всё равно нет. */
    override fun remaining(duration: Duration): String {
        val seconds = maxOf(1L, duration.inWholeSeconds)
        if (seconds < SECONDS_IN_MINUTE) return measures(Measure(seconds, MeasureUnit.SECOND))

        return measures(minute(ceil(seconds.toDouble() / SECONDS_IN_MINUTE).toLong()))
    }

    private fun minute(value: Long) = Measure(value, MeasureUnit.MINUTE)

    private fun measures(vararg values: Measure): String =
        MeasureFormat.getInstance(resources.locale, MeasureFormat.FormatWidth.SHORT)
            .formatMeasures(*values)

    private companion object {
        const val SECONDS_IN_MINUTE = 60L
        const val MINUTES_IN_HOUR = 60L
        const val SECONDS_IN_HOUR = 3600L
    }
}
