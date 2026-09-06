package com.slimly.mediakit.processing

import com.slimly.core.model.ItemProgress
import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingProgress
import com.slimly.core.model.totalSourceByteCount
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Куда очередь сообщает, насколько готов очередной файл. */
internal fun interface ProgressReporting {
    fun update(index: Int, fraction: Double)
}

/**
 * Счётчик прогресса очереди.
 *
 * Доли считаются по весу файлов, а не по их количеству: видео на 600 МБ
 * и фото на 4 МБ — не половина работы каждая, и равномерная полоса на такой
 * паре выглядела бы враньём.
 *
 * Блокировок нет: очередь идёт файл за файлом на одном диспетчере, и звать
 * счётчик с двух потоков некому.
 */
internal class ProgressTracker(
    private val items: List<MediaItem>,
    private val startedAt: Long = System.currentTimeMillis(),
    private val onProgress: (ProcessingProgress) -> Unit,
) : ProgressReporting {
    private val fractions = DoubleArray(items.size)

    /**
     * Доля работы, а не доля веса в галерее: перекодировать придётся ровно
     * то, что уходит в обработку.
     */
    private val weights: DoubleArray = run {
        val total = items.totalSourceByteCount.toDouble()
        if (total > 0) {
            DoubleArray(items.size) { items[it].sourceByteCount / total }
        } else {
            DoubleArray(items.size) { 1.0 / items.size.coerceAtLeast(1) }
        }
    }

    override fun update(index: Int, fraction: Double) {
        if (index !in fractions.indices || fraction <= fractions[index]) return
        fractions[index] = fraction.coerceAtMost(1.0)

        val overall = fractions.indices.sumOf { fractions[it] * weights[it] }.coerceAtMost(1.0)
        onProgress(
            ProcessingProgress(
                overall = overall,
                items = items.mapIndexed { position, item -> ItemProgress(item.id, fractions[position]) },
                estimatedRemaining = remaining(overall),
            ),
        )
    }

    /**
     * Прогноз строим по уже потраченному времени. Пока сделано меньше пяти
     * процентов, цифра прыгала бы от секунд до минут — тогда честнее
     * не показывать её вовсе.
     */
    private fun remaining(overall: Double): Duration? {
        if (overall <= MINIMUM_FOR_ESTIMATE || overall >= 1) return null
        val elapsed = System.currentTimeMillis() - startedAt
        return (elapsed * ((1 - overall) / overall)).milliseconds
    }

    private companion object {
        const val MINIMUM_FOR_ESTIMATE = 0.05
    }
}
