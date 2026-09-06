package com.slimly.core.service

import com.slimly.core.model.ProcessingHistory
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingRecord
import com.slimly.core.model.ProcessingStats
import com.slimly.core.storage.KeyValueStorage
import com.slimly.core.storage.StorageKey
import com.slimly.core.storage.value
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.serializer

internal val processingStatsKey: StorageKey<ProcessingStats> =
    StorageKey.json("slimly.processing.stats", serializer())

internal val processingHistoryKey: StorageKey<ProcessingHistory> =
    StorageKey.json("slimly.processing.history", serializer())

/**
 * Хранилище статистики обработки.
 *
 * Считает только приложение и только за себя: медиатека о наших сжатиях
 * ничего не знает, поэтому цифры профиля живут рядом с настройками,
 * а не выводятся из библиотеки.
 *
 * Хранится и общая сумма, и журнал работ. Сумма не выводится из журнала
 * намеренно: журнал подрезается по краю, а «освобождено с первого запуска»
 * от этого меняться не должно.
 *
 * Изоляцию состояния держит `Mutex` — компилятор за неё, в отличие
 * от иосного `actor`, не отвечает.
 */
class ProcessingStatsStore(private val storage: KeyValueStorage) {
    private val mutex = Mutex()
    private var cached: ProcessingStats? = null
    private var cachedHistory: ProcessingHistory? = null

    suspend fun stats(): ProcessingStats = mutex.withLock { loadStats() }

    suspend fun history(): ProcessingHistory = mutex.withLock { loadHistory() }

    suspend fun add(outcome: ProcessingOutcome): ProcessingStats = mutex.withLock {
        val stats = loadStats().adding(outcome)
        cached = stats
        storage.set(processingStatsKey, stats)

        // В журнал попадает всё, включая скан: разрез по инструментам
        // без него был бы неполным, хотя в весе скану считаться нечем.
        val history = loadHistory().appending(ProcessingRecord.of(outcome))
        cachedHistory = history
        storage.set(processingHistoryKey, history)

        stats
    }

    suspend fun reset() = mutex.withLock {
        cached = ProcessingStats.EMPTY
        cachedHistory = ProcessingHistory.EMPTY
        storage.remove(processingStatsKey)
        storage.remove(processingHistoryKey)
    }

    private suspend fun loadStats(): ProcessingStats =
        cached ?: storage.value(processingStatsKey, ProcessingStats.EMPTY).also { cached = it }

    private suspend fun loadHistory(): ProcessingHistory =
        cachedHistory ?: storage.value(processingHistoryKey, ProcessingHistory.EMPTY).also { cachedHistory = it }
}
