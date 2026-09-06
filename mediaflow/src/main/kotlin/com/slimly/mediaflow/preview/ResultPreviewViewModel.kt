package com.slimly.mediaflow.preview

import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.MediaKind
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.ProcessingResult
import com.slimly.core.service.MediaFileSharing
import com.slimly.core.service.MediaPreviewRenderer
import com.slimly.mediaflow.FlowCopy
import com.slimly.mediaflow.MediaThumbnailLoader
import com.slimly.mediaflow.R
import com.slimly.navigation.Router
import kotlin.math.roundToInt

/** Какую сторону пары «до / после» показываем. */
enum class PreviewSide {
    BEFORE,
    AFTER,
}

/**
 * Просмотр одного готового файла.
 *
 * Экран общий для всех инструментов: различается только то, есть ли с чем
 * сравнивать. У сжатия меняется картинка при том же формате — там нужна пара
 * «оригинал / результат»; у конвертации, скана и звука сравнивать нечего,
 * и остаётся один кадр результата.
 */
class ResultPreviewViewModel(
    val result: ProcessingResult,
    private val kind: ProcessingKind,
    private val thumbnails: MediaThumbnailLoader,
    private val previews: MediaPreviewRenderer?,
    /** Чем файл открывается снаружи: своего плеера и читалки здесь нет. */
    private val sharing: MediaFileSharing,
    private val formatters: SlimlyFormatters,
    private val resources: Resources,
    private val copy: FlowCopy,
    private val router: Router,
) {
    var side: PreviewSide by mutableStateOf(PreviewSide.AFTER)

    /**
     * Где стоит линия шторки: доля ширины окна слева от неё. Пополам —
     * потому что до первого движения ни одной из сторон не отдают
     * предпочтения.
     */
    var comparePosition: Float by mutableStateOf(HALF)

    var beforeFrame: Bitmap? by mutableStateOf(null)
        private set

    var afterFrame: Bitmap? by mutableStateOf(null)
        private set

    private val item = result.item

    /**
     * Оригинал берём из медиатеки, результат — из файла: записью медиатеки он
     * ещё не стал, а если и стал, найти его там по имени было бы гаданием.
     */
    suspend fun load() {
        if (showsSides && beforeFrame == null) {
            beforeFrame = thumbnails.thumbnail(item, BEFORE_PIXEL)
        }

        val output = result.outputUri
        if (afterFrame == null && previews != null && output != null) {
            afterFrame = previews.preview(output, AFTER_PIXEL)
        }
    }

    // MARK: Что показываем

    /**
     * Пара «до / после» есть только там, где результат сопоставим
     * с оригиналом по картинке.
     */
    val showsSides: Boolean
        get() = copy.allowsComparison(kind)

    /**
     * Сравнение идёт прямо в окне — шторкой, а не подменой кадра. Так можно
     * только со снимком: у ролика оба кадра одинаковы на глаз, и разницу
     * там ищут в движении.
     */
    private val comparesInline: Boolean
        get() = showsSides && item.kind == MediaKind.PHOTO

    /**
     * Шторку рисуем, когда приехали оба кадра: до этого делить нечего,
     * и в окне стоит обычная заглушка.
     */
    val showsComparison: Boolean
        get() = comparesInline && beforeFrame != null && afterFrame != null

    /** Переключатель остаётся там, где обе стороны в одно окно не влезают. */
    val showsSidePicker: Boolean
        get() = showsSides && !comparesInline

    val isShowingAfter: Boolean
        get() = !showsSidePicker || side == PreviewSide.AFTER

    val currentFrame: Bitmap?
        get() = if (isShowingAfter) afterFrame else beforeFrame

    /** Пропорции окна просмотра: у видео — кадр, у снимка — лист. */
    val previewAspectRatio: Float
        get() = if (item.kind == MediaKind.VIDEO) VIDEO_ASPECT else PHOTO_ASPECT

    // MARK: Подписи

    /**
     * В заголовке — имя готового файла: у конвертации и скана оно другое,
     * и именно его человек будет искать в «Файлах».
     */
    val title: String
        get() = result.outputName

    val subtitle: String
        get() {
            // У дорожки своего кадра нет, и размер кадра исходника к ней
            // отношения не имеет: подписываем, откуда она и сколько длится.
            if (kind == ProcessingKind.AUDIO_EXTRACTION) return trackSubtitle

            return listOf(
                formatters.items.kindLabel(item),
                item.pixelSizeText,
                formatters.items.meta(item),
            ).filter { it.isNotEmpty() }.joinToString(separator = " · ")
        }

    private val trackSubtitle: String
        get() {
            val duration = item.duration ?: return resources.getString(R.string.preview_track)
            return resources.getString(R.string.preview_track_time, formatters.durations.string(duration))
        }

    val beforeText: String
        get() = formatters.sizes.string(item.byteCount)

    val afterText: String
        get() = formatters.sizes.string(result.outputByteCount)

    val deltaText: String
        get() = formatters.items.reduction(result)

    /**
     * Значок дельты уместен только у сжатия: у конвертации вес меняется
     * как придётся, и «+140%» читалось бы как ошибка, а не как факт.
     */
    val showsDelta: Boolean
        get() = kind == ProcessingKind.COMPRESSION

    /**
     * Расширение готового файла: «PDF», «HEIC». Формат обработки в маршрут
     * не приезжает, а имя файла его уже несёт.
     */
    val outputFormat: String
        get() = result.outputName.substringAfterLast(delimiter = '.', missingDelimiterValue = "").uppercase()

    val beforeTitle: String
        get() = resources.getString(R.string.preview_before_title, beforeText)

    val afterTitle: String
        get() = resources.getString(R.string.preview_after_title, afterText)

    /**
     * Пока кадр не приехал, подписываем, что именно грузится: пустая
     * штриховка без подписи читалась бы как ошибка.
     */
    val placeholderLabel: String
        get() {
            if (!isShowingAfter) return beforePlaceholderLabel

            return when (kind) {
                ProcessingKind.COMPRESSION -> resources.getString(R.string.badge_compressed)

                ProcessingKind.CONVERSION ->
                    outputFormat.ifEmpty { resources.getString(R.string.badge_result) }

                ProcessingKind.SCAN -> resources.getString(R.string.badge_document)

                ProcessingKind.AUDIO_EXTRACTION -> resources.getString(R.string.badge_track)
            }
        }

    /** Левая половина шторки — всегда оригинал. */
    val beforePlaceholderLabel: String
        get() = resources.getString(R.string.badge_original)

    /**
     * У шторки подписи стоят по обе стороны от линии, поэтому каждая
     * собирается сама по себе, а не выбирается по текущей стороне.
     */
    val beforeCaption: String
        get() = resources.getString(R.string.preview_before_caption, beforeText)

    val afterCaption: String
        get() = if (kind == ProcessingKind.COMPRESSION) {
            resources.getString(R.string.preview_after_caption, afterText)
        } else {
            "${outputFormat.lowercase()} · $afterText"
        }

    val caption: String
        get() = if (isShowingAfter) afterCaption else beforeCaption

    val note: String
        get() {
            if (!isShowingAfter) return resources.getString(R.string.preview_source_compare)

            return when (kind) {
                // Доля собирается отдельной строкой: литеральный «%» рядом
                // с числом превратил бы ключ в неверный формат-шаблон.
                ProcessingKind.COMPRESSION ->
                    resources.getString(R.string.preview_compression, "${(result.reduction * PERCENT).roundToInt()}%")

                ProcessingKind.CONVERSION -> resources.getString(R.string.preview_conversion, outputFormat)

                ProcessingKind.SCAN -> resources.getString(R.string.preview_scan, outputFormat)

                ProcessingKind.AUDIO_EXTRACTION -> resources.getString(R.string.preview_audio)
            }
        }

    /**
     * Открыть готовый файл системным приложением: PDF — просмотрщиком,
     * дорожку — плеером. Это и есть обещанное «смотреть и слушать
     * по-настоящему человек идёт в системное приложение»: своего
     * проигрывателя в просмотре нет намеренно — иосные вьюеры AVKit
     * и PDFKit сюда не переехали.
     *
     * `null` — открывать нечем: подходящего приложения на устройстве нет,
     * и кнопку, которая ответит отказом, показывать не надо.
     */
    // Считаем один раз: за ответом «есть ли чем открыть» стоит опрос
    // установленных приложений, а файл у экрана всё равно один.
    val openIntent: Intent? by lazy { result.outputUri?.let { sharing.viewIntent(it) } }

    val openTitle: String
        get() = resources.getString(R.string.preview_open)

    fun goBack() {
        router.pop()
    }

    private companion object {
        const val BEFORE_PIXEL = 420
        const val AFTER_PIXEL = 1400
        const val VIDEO_ASPECT = 16f / 9f
        const val PHOTO_ASPECT = 3f / 4f
        const val HALF = 0.5f
        const val PERCENT = 100
    }
}
