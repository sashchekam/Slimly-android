package com.slimly.core.model

import com.slimly.core.storage.InstantAsMillisSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Одна законченная работа: что сделали, когда и над чем.
 *
 * Общей суммы в профиле хватало, пока вопрос был один — «сколько всего
 * освобождено». На вопрос «чем пользовались за последние 90 дней» сумма
 * не отвечает, поэтому рядом с ней лежит список записей с датами.
 *
 * Запись пишется на всю пачку разом, а не на каждый файл: обработку
 * запускают один раз, и в отчёте это одна работа с несколькими
 * результатами, а не десять отдельных.
 */
@Serializable
data class ProcessingRecord(
    @Serializable(with = InstantAsMillisSerializer::class)
    val date: Instant,
    val kind: ProcessingKind,
    /**
     * Сколько результатов легло в счётчик операций: файлов, а у скана —
     * собранных документов.
     */
    val count: Int,
    val originalByteCount: Long,
    val outputByteCount: Long,
    val photoCount: Int,
    val videoCount: Int,
    /** Страниц в собранном PDF — только у скана. */
    val pageCount: Int,
    /**
     * Длительность исходников в секундах — только у извлечения звука.
     * Секунды, а не `Duration`: запись уезжает в хранилище как JSON.
     */
    val seconds: Double,
    /**
     * Формат исходников: «HEIC», «MP4». У сжатия формат не меняется, поэтому
     * пары «из → в» нет и строка пуста.
     */
    val sourceFormat: String,
    /** Формат результата: «JPEG», «M4A», «PDF». */
    val targetFormat: String,
) {
    /** «HEIC → JPEG». Пусто, если формат не менялся или система его не назвала. */
    val formatPair: String
        get() = if (sourceFormat.isNotEmpty() && targetFormat.isNotEmpty()) {
            "$sourceFormat → $targetFormat"
        } else {
            ""
        }

    companion object {
        private const val MILLIS_IN_SECOND = 1000.0

        /**
         * Запись по итогу обработки. Считается по сохранённым результатам,
         * а не по заданию: профиль показывает сделанное, и отказ по файлу
         * в отчёт попасть не должен.
         */
        fun of(outcome: ProcessingOutcome, date: Instant = Instant.now()): ProcessingRecord {
            val results = outcome.results
            return ProcessingRecord(
                date = date,
                kind = outcome.kind,
                count = results.size,
                originalByteCount = results.originalByteCount,
                outputByteCount = results.outputByteCount,
                photoCount = results.count { it.item.kind == MediaKind.PHOTO },
                videoCount = results.count { it.item.kind == MediaKind.VIDEO },
                // Страницы считаются по заданию, а не по результатам: из пачки
                // снимков выходит один PDF, и в результатах их уже не видно.
                pageCount = if (outcome.kind == ProcessingKind.SCAN) outcome.request.items.size else 0,
                seconds = if (outcome.kind == ProcessingKind.AUDIO_EXTRACTION) {
                    results.sumOf { (it.item.duration?.inWholeMilliseconds ?: 0L) / MILLIS_IN_SECOND }
                } else {
                    0.0
                },
                sourceFormat = outcome.request.sourceFormat,
                targetFormat = outcome.request.targetFormat?.shortName.orEmpty(),
            )
        }
    }
}
