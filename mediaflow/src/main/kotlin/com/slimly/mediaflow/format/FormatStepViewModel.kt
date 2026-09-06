package com.slimly.mediaflow.format

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.MediaFormat
import com.slimly.core.model.MediaItem
import com.slimly.core.model.ProcessingOperation
import com.slimly.core.model.ProcessingRequest
import com.slimly.core.model.totalByteCount
import com.slimly.mediaflow.FlowDestination
import com.slimly.mediaflow.MediaThumbnailLoader
import com.slimly.mediaflow.go
import com.slimly.navigation.Route
import com.slimly.navigation.Router

/** Правила шага «в какой формат» для конкретного инструмента. */
data class FormatStepConfig(
    val title: String,
    val note: String,
    /** Глагол на кнопке: «Конвертировать», «Извлечь звук», «Собрать скан». */
    val actionTitle: String,
    /** Формат исходников — левая половина пары «из → в». */
    val sourceFormat: String,
    val formats: List<MediaFormat>,
    val noun: CountNoun,
    /**
     * Показывать ли вес: у скана результат ценен читаемостью, а не
     * мегабайтами, и прогноз «5 МБ → ≈ 0,1 МБ» там ничего не решает.
     */
    val showsSize: Boolean = true,
    /**
     * Что именно делать с выбранным форматом. Шаг один и тот же, а работа
     * за ним разная — поэтому её задаёт инструмент.
     */
    val makeOperation: (MediaFormat) -> ProcessingOperation,
)

class FormatStepViewModel(
    val config: FormatStepConfig,
    val items: List<MediaItem>,
    val thumbnails: MediaThumbnailLoader,
    private val formatters: SlimlyFormatters,
    private val router: Router,
    private val run: (ProcessingRequest) -> Route,
    /**
     * Куда ведёт нажатие на файл в списке. `null` — просмотра нет, и строка
     * остаётся просто строкой.
     */
    private val preview: ((MediaItem) -> FlowDestination)? = null,
) {
    /**
     * Исходный формат из списка убираем: «сконвертировать HEIC в HEIC» —
     * не выбор, а недоразумение. Сравниваем без учёта регистра: слева стоит
     * расширение файла, справа — имя формата, и «webp» с «WebP» — это одно
     * и то же.
     */
    val formats: List<MediaFormat> =
        config.formats.filterNot { it.shortName.equals(config.sourceFormat, ignoreCase = true) }

    var selected: MediaFormat by mutableStateOf(formats.firstOrNull() ?: config.formats.first())
        private set

    fun select(format: MediaFormat) {
        selected = format
    }

    fun isSelected(format: MediaFormat): Boolean = format == selected

    // MARK: Показатели

    val summary: String
        get() {
            val count = formatters.counts.string(items.size, config.noun)
            if (!config.showsSize) return count
            return "$count · ${formatters.sizes.string(items.totalByteCount)}"
        }

    val targetTitle: String
        get() = selected.shortName

    val ctaTitle: String
        get() = "${config.actionTitle} · ${formatters.counts.string(items.size, config.noun)}"

    /**
     * Строка под именем файла — «было → станет». Там, где вес не показываем,
     * остаётся размер кадра: по нему видно, что за файл, а число мегабайт —
     * нет.
     */
    fun detail(item: MediaItem): String {
        if (!config.showsSize) {
            // Размер кадра система сообщает не всегда, а пустая вторая строка
            // роняет высоту строки списка — тогда подписываем хотя бы тип.
            return item.pixelSizeText.ifEmpty { formatters.items.kindLabel(item) }
        }

        val estimate = formatters.sizes.string(request.estimatedByteCount(item))
        return "${formatters.sizes.string(item.byteCount)} → ≈ $estimate"
    }

    // MARK: Навигация

    val request: ProcessingRequest
        get() = ProcessingRequest(
            items = items,
            operation = config.makeOperation(selected),
            sourceFormat = config.sourceFormat,
        )

    /**
     * Открыть исходник до обработки: на этом шаге от файла остаются имя
     * и миниатюра в 33 точки, и по ним не всегда видно, тот ли это кадр.
     */
    val allowsPreview: Boolean
        get() = preview != null

    fun openPreview(item: MediaItem) {
        router.go(preview?.invoke(item) ?: return)
    }

    fun goBack() {
        router.pop()
    }

    fun start() {
        router.push(run(request))
    }
}
