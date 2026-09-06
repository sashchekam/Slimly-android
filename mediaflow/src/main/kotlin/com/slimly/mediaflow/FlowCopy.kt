package com.slimly.mediaflow

import android.content.res.Resources
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.savedByteCount

/**
 * Тексты общих шагов флоу.
 *
 * Инструменты идут одной дорогой и различаются только тем, что делают
 * с файлами, — поэтому подписи шагов выводятся из [ProcessingKind],
 * а не переписываются в каждой фиче.
 */
class FlowCopy(private val resources: Resources, private val formatters: SlimlyFormatters) {
    /** Подпись под процентом на экране обработки. */
    fun runWord(kind: ProcessingKind): String = resources.getString(
        when (kind) {
            ProcessingKind.COMPRESSION -> R.string.stage_compressing
            ProcessingKind.CONVERSION -> R.string.stage_converting
            ProcessingKind.AUDIO_EXTRACTION -> R.string.stage_extracting
            ProcessingKind.SCAN -> R.string.stage_scanning
        },
    )

    /** Кикер над итогом. */
    fun doneKicker(kind: ProcessingKind): String = resources.getString(
        when (kind) {
            ProcessingKind.COMPRESSION -> R.string.stage_compression_done
            ProcessingKind.CONVERSION -> R.string.stage_conversion_done
            ProcessingKind.AUDIO_EXTRACTION -> R.string.stage_audio_done
            ProcessingKind.SCAN -> R.string.stage_scan_done
        },
    )

    /** Строка под крупной цифрой итога. */
    fun summary(outcome: ProcessingOutcome): String {
        val results = outcome.results
        val files = formatters.counts.string(results.size, CountNoun.FILE)

        return when (outcome.kind) {
            ProcessingKind.COMPRESSION -> compressionSummary(outcome, files)

            else -> {
                val target = outcome.request.targetFormat?.shortName.orEmpty()
                resources.getString(R.string.summary_conversion, files, target)
            }
        }
    }

    /**
     * Уже плотно упакованный файл после пережатия может даже потяжелеть.
     * Писать «освободили −0,2 МБ» нельзя: это не итог, а издевательство.
     */
    private fun compressionSummary(outcome: ProcessingOutcome, files: String): String {
        val saved = outcome.results.savedByteCount
        if (saved <= 0) return resources.getString(R.string.summary_dense, files)

        return listOf(
            resources.getString(R.string.summary_freed, formatters.sizes.string(saved)),
            files,
            resources.getString(R.string.summary_originals_untouched),
        ).joinToString(separator = " · ")
    }

    /**
     * Показывать ли предложение удалить оригиналы.
     *
     * Только там, где оригинал действительно стал лишним: после извлечения
     * звука видео нужно как было, а скан оригиналы вообще не заменяет.
     */
    fun offersDeletingOriginals(kind: ProcessingKind): Boolean =
        kind == ProcessingKind.COMPRESSION || kind == ProcessingKind.CONVERSION

    /**
     * Подсказка под списком готовых файлов.
     *
     * У сжатия смотрят пару «до / после», остальное просто открывают — звать
     * «сравнить» везде было бы враньём. Дорожку на iOS зовут послушать,
     * а здесь плеера в просмотре нет: слушают её системным приложением,
     * и обещать прослушивание внутри мы не можем.
     */
    fun previewHint(kind: ProcessingKind): String = resources.getString(
        when (kind) {
            ProcessingKind.COMPRESSION -> R.string.hint_compression
            ProcessingKind.CONVERSION, ProcessingKind.AUDIO_EXTRACTION, ProcessingKind.SCAN -> R.string.hint_preview
        },
    )

    /**
     * Сравнение «до / после» имеет смысл только у сжатия: там меняется
     * картинка, а не контейнер.
     */
    fun allowsComparison(kind: ProcessingKind): Boolean = kind == ProcessingKind.COMPRESSION
}
