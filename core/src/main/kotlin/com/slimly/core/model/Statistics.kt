package com.slimly.core.model

import kotlinx.serialization.Serializable

/**
 * Что о медиатеке отдаёт система.
 *
 * Общего веса медиатеки не сообщает ни одна платформа, поэтому его здесь нет
 * и быть не может: доступны только количества и свободное место.
 */
data class LibrarySnapshot(val photoCount: Int, val videoCount: Int, val freeSpace: Long)

/**
 * Что мы обработали сами — единственные цифры о весе, за которые можем
 * отвечать.
 *
 * Вес считаем только по сжатию: конвертация может файл и утяжелить (PNG
 * из HEIC), поэтому её складывать в «освобождено» нельзя — она идёт
 * отдельным счётчиком.
 */
@Serializable
data class ProcessingStats(
    val originalByteCount: Long,
    val compressedByteCount: Long,
    val photoCount: Int,
    val videoCount: Int,
    val conversionCount: Int,
) {
    val savedByteCount: Long
        get() = originalByteCount - compressedByteCount

    val fileCount: Int
        get() = photoCount + videoCount

    /**
     * Доля оставшегося веса — ширина полосы «стало / было». Ограничена
     * единицей: полоса не умеет быть длиннее себя, а такой случай возможен —
     * плотный файл после пережатия иногда тяжелеет.
     */
    val remainingFraction: Double
        get() = if (originalByteCount > 0) {
            minOf(compressedByteCount.toDouble() / originalByteCount, 1.0)
        } else {
            0.0
        }

    /** Присоединить итог очередной обработки. */
    fun adding(outcome: ProcessingOutcome): ProcessingStats = when (outcome.kind) {
        // Скан в счётчиках не отражается: из пачки снимков получается один
        // PDF, и он не конвертация — считать его наравне с ней значило бы
        // показывать в профиле цифру, которой там нет смысла.
        ProcessingKind.SCAN -> this

        ProcessingKind.COMPRESSION -> copy(
            originalByteCount = originalByteCount + outcome.results.originalByteCount,
            compressedByteCount = compressedByteCount + outcome.results.outputByteCount,
            photoCount = photoCount + outcome.results.count { it.item.kind == MediaKind.PHOTO },
            videoCount = videoCount + outcome.results.count { it.item.kind == MediaKind.VIDEO },
        )

        else -> copy(conversionCount = conversionCount + outcome.results.size)
    }

    companion object {
        val EMPTY = ProcessingStats(
            originalByteCount = 0,
            compressedByteCount = 0,
            photoCount = 0,
            videoCount = 0,
            conversionCount = 0,
        )
    }
}
