package com.slimly.core.model

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.StringRes
import com.slimly.core.R
import com.slimly.core.format.CountNoun
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * Что именно делаем с файлами.
 *
 * Все инструменты идут одним путём «выбор → настройки → обработка → итог»,
 * и различаются как раз этим: от вида работы зависят и тексты шагов, и то,
 * как результат ложится в статистику.
 */
enum class ProcessingKind(
    /**
     * Как работа называется в отчёте о сделанном.
     *
     * Каталог инструментов сюда не годится: там пять карточек, а конвертация
     * фото и видео — одна и та же работа, и в статистике они складываются.
     */
    @get:StringRes val titleRes: Int,
    /** Имя для тесных мест — легенды под полосой долей, где полное не помещается. */
    @get:StringRes val shortTitleRes: Int,
    /**
     * Что считает счётчик операций: сжатие и конвертация работают с файлами,
     * звук достаётся из видео, а скан считается документами, а не страницами.
     */
    val unit: CountNoun,
) {
    COMPRESSION(R.string.kind_compression, R.string.kind_short_compression, CountNoun.FILE),
    CONVERSION(R.string.kind_conversion, R.string.kind_short_conversion, CountNoun.FILE),
    AUDIO_EXTRACTION(R.string.kind_audio_extraction, R.string.kind_short_audio_extraction, CountNoun.VIDEO),
    SCAN(R.string.kind_scan, R.string.kind_short_scan, CountNoun.SCAN),
}

// MARK: - Форматы результата

/**
 * Формат изображения, который система умеет записать.
 *
 * WebP здесь есть, в отличие от iOS: `Bitmap.compress` пишет его свободно,
 * а ImageIO на iOS только читает. HEIC наоборот — пишется лишь там, где
 * у устройства есть аппаратный кодировщик, и решает это уже `:mediakit`.
 */
enum class ImageFormat(val displayName: String, val fileExtension: String) {
    JPEG("JPEG", "jpg"),
    HEIC("HEIC", "heic"),
    PNG("PNG", "png"),
    WEBP("WebP", "webp"),
    ;

    /** У PNG качество не регулируется — он без потерь. */
    val isLossy: Boolean
        get() = this != PNG
}

/**
 * Контейнер видео. Один: MOV с iOS сюда не переезжает — системный
 * упаковщик Android его не пишет, и предлагать такой пункт было бы обманом.
 * Второй появится в фазе 3, если его даст Media3 Transformer.
 */
enum class VideoContainer(val displayName: String, val fileExtension: String) {
    MP4("MP4", "mp4"),
}

enum class VideoCodec(val displayName: String) {
    H264("H.264"),
    HEVC("HEVC"),
}

/**
 * Формат звуковой дорожки.
 *
 * MP3 не предлагаем: кодировщика MP3 в системе нет — ни здесь, ни на iOS.
 */
sealed interface AudioFormat : Parcelable {
    val fileExtension: String

    /** Имя контейнера без настроек — то, что уходит в журнал работ. */
    val containerName: String

    /**
     * Байт в секунду — по нему считается оценка веса дорожки.
     * Для WAV это 44,1 кГц × 16 бит × 2 канала.
     */
    val bytesPerSecond: Double

    /** AAC в контейнере M4A — родной для Android. */
    @Parcelize
    data class Aac(val kilobits: Int) : AudioFormat {
        override val fileExtension: String get() = "m4a"
        override val containerName: String get() = "M4A"
        override val bytesPerSecond: Double get() = kilobits * BITS_IN_KILOBIT / BITS_IN_BYTE
    }

    /** WAV без потерь. */
    @Parcelize
    data object LinearPcm : AudioFormat {
        override val fileExtension: String get() = "wav"
        override val containerName: String get() = "WAV"
        override val bytesPerSecond: Double get() = SAMPLE_RATE * BYTES_PER_SAMPLE * CHANNELS
    }

    private companion object {
        const val BITS_IN_KILOBIT = 1000.0
        const val BITS_IN_BYTE = 8.0
        const val SAMPLE_RATE = 44_100.0
        const val BYTES_PER_SAMPLE = 2.0
        const val CHANNELS = 2.0
    }
}

/**
 * Во что превращаем файл. Отсюда обработчик берёт настройки кодировщика,
 * а интерфейс — подпись формата.
 */
sealed interface ProcessingTarget : Parcelable {
    /**
     * Короткое имя формата — без локали: оно уезжает в журнал работ и должно
     * читаться одинаково независимо от языка, на котором сделали обработку.
     */
    val shortName: String

    @Parcelize
    data class Image(val format: ImageFormat) : ProcessingTarget {
        override val shortName: String get() = format.displayName
    }

    /**
     * Имя формата здесь — кодек, а не контейнер: контейнер на Android один,
     * и подписать им оба варианта значило бы предложить «MP4 или MP4».
     * Выбирают тут именно кодек, и он же уезжает в журнал работ.
     */
    @Parcelize
    data class Video(val container: VideoContainer, val codec: VideoCodec) : ProcessingTarget {
        override val shortName: String get() = codec.displayName
    }

    @Parcelize
    data class Audio(val format: AudioFormat) : ProcessingTarget {
        override val shortName: String get() = format.containerName
    }

    /**
     * PDF из страниц скана. Режим — часть цели, а не отдельная настройка:
     * от него зависит то, как выглядит собранная страница.
     */
    @Parcelize
    data class Document(val mode: ScanMode) : ProcessingTarget {
        override val shortName: String get() = "PDF"
    }
}

/**
 * Формат результата вместе с оценкой веса.
 *
 * Коэффициент — доля исходного веса относительно оригинала; у PNG он больше
 * единицы, и это честно: конвертация не всегда облегчает файл. Там, где вес
 * не показывают и не считают, оценки нет — коэффициент остаётся единицей.
 */
@Parcelize
data class MediaFormat(val target: ProcessingTarget, val ratio: Double = 1.0) : Parcelable {
    val shortName: String
        get() = target.shortName
}

// MARK: - Степень сжатия

/** Пресет сжатия. Коэффициент — доля исходного веса, которая останется. */
enum class CompressionPreset(val ratio: Double, @get:StringRes val titleRes: Int, @get:StringRes val summaryRes: Int) {
    LIGHT(0.68, R.string.quality_light, R.string.quality_summary_light),
    MEDIUM(0.42, R.string.quality_medium, R.string.quality_summary_medium),
    MAXIMUM(0.24, R.string.quality_maximum, R.string.quality_summary_maximum),
}

/**
 * Положение ползунка степени сжатия.
 *
 * Пресеты — это засечки на непрерывной шкале, а не три отдельных режима:
 * между ними коэффициент интерполируется, поэтому промежуточное положение
 * ползунка тоже имеет смысл, а не «прилипает» к ближайшему пресету.
 */
// Конструктор закрыт, чтобы значение всегда лежало в границах шкалы:
// собирают его `of` и `at`. `copy()` закрывается вместе с ним.
@ConsistentCopyVisibility
@Parcelize
data class CompressionLevel private constructor(val value: Double) : Parcelable {
    val ratio: Double
        get() {
            val presets = CompressionPreset.entries
            val scaled = value * (presets.size - 1)
            val index = minOf(scaled.toInt(), presets.size - 2)
            val fraction = scaled - index
            val from = presets[index].ratio
            return from + fraction * (presets[index + 1].ratio - from)
        }

    /** Пресет, подпись которого подсвечена над ползунком. */
    val nearestPreset: CompressionPreset
        get() {
            val presets = CompressionPreset.entries
            val index = (value * (presets.size - 1)).roundToInt()
            return presets[index.coerceIn(0, presets.size - 1)]
        }

    companion object {
        val DEFAULT = CompressionLevel(0.5)

        fun of(value: Double): CompressionLevel = CompressionLevel(value.coerceIn(0.0, 1.0))

        fun at(preset: CompressionPreset): CompressionLevel {
            val presets = CompressionPreset.entries
            return CompressionLevel(presets.indexOf(preset).toDouble() / (presets.size - 1))
        }
    }
}

// MARK: - Задание

/**
 * Работа, которую нужно проделать над файлами.
 *
 * Настройки кодировщика выводятся отсюда, а не приходят числом «доля веса»:
 * коэффициент — это оценка для интерфейса, а обработке нужно знать формат,
 * кодек и качество.
 */
sealed interface ProcessingOperation : Parcelable {
    val kind: ProcessingKind

    /** У сжатия формат не меняется, поэтому пары «из → в» нет. */
    val targetFormat: MediaFormat?

    /** Доля исходного веса, которая останется, — оценка до обработки. */
    val estimatedRatio: Double

    /** Сжатие без смены формата — исходный контейнер сохраняется. */
    @Parcelize
    data class Compress(val level: CompressionLevel) : ProcessingOperation {
        override val kind: ProcessingKind get() = ProcessingKind.COMPRESSION
        override val targetFormat: MediaFormat? get() = null
        override val estimatedRatio: Double get() = level.ratio
    }

    @Parcelize
    data class Convert(val format: MediaFormat) : ProcessingOperation {
        override val kind: ProcessingKind get() = ProcessingKind.CONVERSION
        override val targetFormat: MediaFormat get() = format
        override val estimatedRatio: Double get() = format.ratio
    }

    @Parcelize
    data class ExtractAudio(val format: MediaFormat) : ProcessingOperation {
        override val kind: ProcessingKind get() = ProcessingKind.AUDIO_EXTRACTION
        override val targetFormat: MediaFormat get() = format
        override val estimatedRatio: Double get() = format.ratio
    }

    /**
     * Сборка PDF из страниц. Границы листа идут вместе с форматом: их либо
     * нашла автоматика, либо поправил человек, и обработка не должна гадать,
     * чьи они, — она просто берёт то, что ей дали.
     */
    @Parcelize
    data class Scan(val format: MediaFormat, val bounds: ScanBounds) : ProcessingOperation {
        override val kind: ProcessingKind get() = ProcessingKind.SCAN
        override val targetFormat: MediaFormat get() = format
        override val estimatedRatio: Double get() = format.ratio
    }
}

/** Задание на обработку — всё, что нужно знать сервису и экранам после него. */
@Parcelize
data class ProcessingRequest(
    val items: List<MediaItem>,
    val operation: ProcessingOperation,
    /**
     * Исходный формат: «HEIC», «MP4». Показываем в паре «из → в». У сжатия
     * формат не меняется, поэтому пара не нужна и строка пуста.
     */
    val sourceFormat: String = "",
) : Parcelable {
    val kind: ProcessingKind
        get() = operation.kind

    val ratio: Double
        get() = operation.estimatedRatio

    val targetFormat: MediaFormat?
        get() = operation.targetFormat

    val originalByteCount: Long
        get() = items.totalByteCount

    /**
     * Вес «после» до обработки — оценка, поэтому в интерфейсе всегда подписан
     * как приблизительный.
     */
    val estimatedByteCount: Long
        get() = items.sumOf { estimatedByteCount(it) }

    /**
     * Вес дорожки считаем по битрейту и длительности, а не долей от видео:
     * час разговора и час съёмки 4K дают одинаковый звук и разные видео.
     */
    fun estimatedByteCount(item: MediaItem): Long {
        val operation = operation
        val duration = item.duration
        if (operation is ProcessingOperation.ExtractAudio && duration != null) {
            val target = operation.format.target
            if (target is ProcessingTarget.Audio) {
                return (target.format.bytesPerSecond * duration.inWholeMilliseconds / MILLIS_IN_SECOND).toLong()
            }
        }
        // Считаем от того файла, который реально пойдёт в кодировщик:
        // у обрезанного видео это рендер правок, а не исходник с камеры.
        return (item.sourceByteCount * ratio).toLong()
    }

    /** «−58%». У конвертации может получиться и прибавка — знак берём из числа. */
    val deltaPercent: Int
        get() {
            if (originalByteCount <= 0) return 0
            val remaining = estimatedByteCount.toDouble() / originalByteCount
            return ((1 - remaining) * PERCENT).roundToInt()
        }

    private companion object {
        const val MILLIS_IN_SECOND = 1000.0
        const val PERCENT = 100
    }
}

// MARK: - Результат

/** Куда лёг готовый файл. */
enum class OutputDestination {
    GALLERY,

    /**
     * Общие коллекции, которые показывают «Файлы»: документ ложится
     * в `Documents/Slimly`, дорожка — в `Music/Slimly`. Туда, чему
     * в галерее не место.
     */
    FILES,
}

@Parcelize
data class ProcessingResult(
    val item: MediaItem,
    val outputByteCount: Long,
    /** Имя готового файла — у конвертации и звука оно отличается от исходного. */
    val outputName: String,
    /**
     * Готовый файл в кэше приложения. Пока результат не сохранён, это
     * единственное место, где он есть; после сохранения — копия, по которой
     * показываем «после» в сравнении.
     */
    val outputUri: Uri? = null,
    /** Куда результат уедет, когда его сохранят. */
    val destination: OutputDestination = OutputDestination.GALLERY,
) : Parcelable {
    val savedByteCount: Long
        get() = item.byteCount - outputByteCount

    /** Доля, на которую файл похудел: 0.64 — это «−64%». */
    val reduction: Double
        get() = if (item.byteCount > 0) 1 - outputByteCount.toDouble() / item.byteCount else 0.0
}

/**
 * Файл, который обработать не удалось.
 *
 * Отказ по одному файлу не отменяет остальные: медиатека умеет отдать битый
 * файл или тот, что ещё не докачался из облака.
 */
@Parcelize
data class ProcessingFailure(val item: MediaItem, val message: String) : Parcelable

val Iterable<ProcessingResult>.originalByteCount: Long
    get() = sumOf { it.item.byteCount }

val Iterable<ProcessingResult>.outputByteCount: Long
    get() = sumOf { it.outputByteCount }

val Iterable<ProcessingResult>.savedByteCount: Long
    get() = sumOf { it.savedByteCount }

// MARK: - Прогресс

data class ItemProgress(val id: String, val fraction: Double)

data class ProcessingProgress(
    val overall: Double,
    val items: List<ItemProgress>,
    /** `null`, пока оценивать не по чему: первые доли секунды прогноз был бы выдумкой. */
    val estimatedRemaining: Duration?,
) {
    fun fraction(item: MediaItem): Double = items.firstOrNull { it.id == item.id }?.fraction ?: 0.0

    companion object {
        fun idle(items: List<MediaItem>): ProcessingProgress = ProcessingProgress(
            overall = 0.0,
            items = items.map { ItemProgress(id = it.id, fraction = 0.0) },
            estimatedRemaining = null,
        )
    }
}

// MARK: - Итог

/**
 * Чем закончилась обработка. Задание носим с собой: экран итога говорит
 * о результате по-разному в зависимости от того, что именно делали.
 */
@Parcelize
data class ProcessingOutcome(
    val request: ProcessingRequest,
    val results: List<ProcessingResult>,
    val failures: List<ProcessingFailure> = emptyList(),
) : Parcelable {
    val kind: ProcessingKind
        get() = request.kind

    /** Ни одного готового файла — итог показываем как неудачу. */
    val isEmpty: Boolean
        get() = results.isEmpty()
}

/**
 * Работа, которая идёт прямо сейчас.
 *
 * Отдельно от [ProcessingEvent], потому что смотрит на неё не экран,
 * а уведомление: пока приложение свёрнуто, подписчика у событий нет,
 * а работа продолжается и должна быть видна в шторке.
 */
data class ProcessingRun(val request: ProcessingRequest, val progress: ProcessingProgress)

sealed interface ProcessingEvent {
    data class Progress(val progress: ProcessingProgress) : ProcessingEvent

    data class Finished(val outcome: ProcessingOutcome) : ProcessingEvent
}
