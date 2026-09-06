package com.slimly.mediaflow.outcome

import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.OutputDestination
import com.slimly.core.model.ProcessingFailure
import com.slimly.core.model.ProcessingKind
import com.slimly.core.model.ProcessingOutcome
import com.slimly.core.model.ProcessingResult
import com.slimly.core.model.originalByteCount
import com.slimly.core.model.outputByteCount
import com.slimly.core.service.MediaFileSharing
import com.slimly.core.service.MediaLibrary
import com.slimly.core.service.MediaProcessingService
import com.slimly.mediaflow.FlowAnalytics
import com.slimly.mediaflow.FlowCopy
import com.slimly.mediaflow.FlowDestination
import com.slimly.mediaflow.MediaThumbnailLoader
import com.slimly.mediaflow.R
import com.slimly.mediaflow.go
import com.slimly.mediaflow.thumbnailLoader
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OutcomeViewModel(
    outcome: ProcessingOutcome,
    private val library: MediaLibrary,
    private val service: MediaProcessingService,
    private val formatters: SlimlyFormatters,
    private val resources: Resources,
    private val copy: FlowCopy,
    private val analytics: FlowAnalytics,
    private val router: Router,
    private val scope: CoroutineScope,
    /** Чем готовый файл выходит наружу: шторка «Поделиться» и «Открыть в…». */
    private val sharing: MediaFileSharing,
    /**
     * Куда ведёт нажатие на готовый файл. `null` — просмотра у инструмента
     * нет, и строки списка остаются просто строками.
     */
    private val preview: ((ProcessingResult) -> FlowDestination)? = null,
) {
    /**
     * Итог меняется на месте: сохранение переписывает ссылки на файлы,
     * а то, что сохранить не удалось, переезжает в список отказов.
     */
    var outcome: ProcessingOutcome by mutableStateOf(outcome)
        private set

    var isSaving: Boolean by mutableStateOf(false)
        private set

    var isSaved: Boolean by mutableStateOf(false)
        private set

    /**
     * Подтверждение сохранения. Живёт пару секунд: где искать файлы, написано
     * и в подписи под списком — тост только отмечает момент.
     */
    @get:StringRes
    var toast: Int? by mutableStateOf(null)
        private set

    /**
     * Системный запрос на удаление оригиналов. Показать его может только
     * Activity, поэтому вьюмодель кладёт сюда намерение, а запускает
     * его экран.
     */
    var deleteRequest: IntentSender? by mutableStateOf(null)
        private set

    var isDeleting: Boolean by mutableStateOf(false)
        private set

    val thumbnails: MediaThumbnailLoader = library.thumbnailLoader

    private var toastJob: Job? = null

    val results: List<ProcessingResult>
        get() = outcome.results

    val failures: List<ProcessingFailure>
        get() = outcome.failures

    // MARK: Заголовок

    val kicker: String
        get() = if (outcome.isEmpty) "" else copy.doneKicker(outcome.kind)

    val beforeText: String
        get() = formatters.sizes.string(results.originalByteCount)

    val afterText: String
        get() = formatters.sizes.string(results.outputByteCount)

    val summary: String
        get() = copy.summary(outcome)

    // MARK: Список файлов

    /**
     * Вторая строка файла. У сжатия это пара «было → стало»: там вес и есть
     * итог. У звука вес один — исходное видео с дорожкой не сравнивают.
     * У конвертации и скана важен формат, а не мегабайты.
     */
    fun detail(result: ProcessingResult): String = when (outcome.kind) {
        ProcessingKind.COMPRESSION ->
            formatters.items.sizeTransition(result.item.byteCount, result.outputByteCount)

        ProcessingKind.AUDIO_EXTRACTION ->
            resources.getString(R.string.outcome_track_size, formatters.sizes.string(result.outputByteCount))

        ProcessingKind.CONVERSION, ProcessingKind.SCAN ->
            "${outcome.request.sourceFormat} → $targetFormat"
    }

    /**
     * Справа от файла. Доля уместна только у сжатия: у конвертации вес
     * меняется как придётся, и «+140%» читалось бы как ошибка, а не как факт, —
     * там на её месте стоит формат результата.
     */
    fun delta(result: ProcessingResult): String =
        if (outcome.kind == ProcessingKind.COMPRESSION) formatters.items.reduction(result) else targetFormat

    private val targetFormat: String
        get() = outcome.request.targetFormat?.shortName.orEmpty()

    // MARK: Просмотр

    /** Открыть просмотр можно, только когда файл ещё есть на диске. */
    fun canPreview(result: ProcessingResult): Boolean = preview != null && result.outputUri != null

    /**
     * Подсказка под списком. Пока файлов нет, её не показываем: подсказывать
     * нажатие на пустой список незачем.
     */
    val previewHint: String?
        get() = if (preview != null && results.isNotEmpty()) copy.previewHint(outcome.kind) else null

    fun openPreview(result: ProcessingResult) {
        val destination = preview?.takeIf { canPreview(result) } ?: return

        analytics.didPreviewResult()
        router.go(destination(result))
    }

    // MARK: Поделиться

    /**
     * Наружу отдаём копию из рабочей папки, а не сохранённый файл: копия
     * есть всегда — и до сохранения тоже, — а адрес записи медиатеки
     * за её пределами ничего не значит.
     */
    fun shareIntent(result: ProcessingResult): Intent? = result.outputUri?.let { sharing.shareIntent(listOf(it)) }

    /**
     * Одна шторка на все результаты: по отдельности их пришлось бы
     * отправлять столько же раз, сколько файлов в очереди.
     */
    fun shareAllIntent(): Intent? = sharing.shareIntent(sharedFiles)

    fun canShare(result: ProcessingResult): Boolean = result.outputUri != null

    /**
     * Общая кнопка нужна, только когда файлов больше одного: при единственном
     * результате она повторяла бы значок в строке.
     */
    val showsShareAll: Boolean
        get() = sharedFiles.size > 1

    val shareAllTitle: String
        get() = resources.getString(R.string.outcome_share_all, sharedFiles.size)

    fun shareFileLabel(result: ProcessingResult): String =
        resources.getString(R.string.outcome_share_file, result.outputName)

    private val sharedFiles: List<Uri>
        get() = results.mapNotNull { it.outputUri }

    // MARK: Сохранение

    /**
     * Куда уедут файлы. Внутри одного заказа место всегда одно: у сжатия
     * и конвертации — галерея, у дорожек и сканов — общие коллекции,
     * которые показывают «Файлы».
     */
    private val destination: OutputDestination
        get() = results.firstOrNull()?.destination ?: OutputDestination.GALLERY

    /**
     * Кнопка живёт до первого успеха: второй раз она сохранила бы вторую
     * копию, а выключенная с надписью «Сохранено» осталась бы на экране
     * мёртвой. После сохранения её место занимает «Готово».
     */
    val showsSaveButton: Boolean
        get() = results.isNotEmpty() && !isSaved

    /**
     * Подпись кнопки называет то место, куда файл на самом деле уедет:
     * «в галерею» на PDF было бы прямым обманом — в галерее его не будет.
     */
    @get:StringRes
    val saveTitle: Int
        get() = when (destination) {
            OutputDestination.GALLERY -> R.string.save_to_gallery
            OutputDestination.FILES -> R.string.save_to_files
        }

    /**
     * Подпись под кнопкой, наоборот, никуда не уходит: до сохранения она
     * предупреждает, после — остаётся единственным местом, где написано,
     * куда легли файлы, когда тост погаснет.
     */
    val showsSaveNote: Boolean
        get() = results.isNotEmpty()

    @get:StringRes
    val saveNote: Int
        get() = when {
            !isSaved -> R.string.save_hint
            destination == OutputDestination.GALLERY -> R.string.save_done
            else -> R.string.save_files_folder
        }

    /**
     * Куда именно легли файлы — в тосте это главное: подтверждение без места
     * ничем не отличалось бы от «готово».
     */
    @get:StringRes
    private val savedToast: Int
        get() = when (destination) {
            OutputDestination.GALLERY -> R.string.save_done
            OutputDestination.FILES -> R.string.save_files_done
        }

    /**
     * Сохранение — отдельное действие человека, а не хвост обработки:
     * до нажатия ни один готовый файл наружу не выходит.
     */
    fun save() {
        if (isSaving || isSaved || results.isEmpty()) return
        isSaving = true
        val requested = results.size

        scope.launch {
            val stored = service.save(outcome)
            outcome = stored
            isSaving = false

            if (stored.results.isEmpty()) {
                analytics.didFailSaving(requested)
                say(R.string.save_failed)
                return@launch
            }

            isSaved = true
            analytics.didSaveResults(stored)

            val lost = requested - stored.results.size
            if (lost > 0) analytics.didFailSaving(lost)
            say(if (lost > 0) R.string.save_partial else savedToast)
        }
    }

    /**
     * Тост уходит сам: следующий показ отменяет прошлый отсчёт, иначе старый
     * таймер погасил бы новое сообщение раньше срока.
     */
    private fun say(@StringRes text: Int) {
        toastJob?.cancel()
        toast = text
        toastJob = scope.launch {
            delay(TOAST_MS)
            toast = null
        }
    }

    // MARK: Имя файла

    /**
     * Какой файл переименовывают — по элементу, из которого он получен:
     * своего ключа у результата нет, а элемент у него всегда один. У скана
     * результат и вовсе единственный.
     */
    private var renamedId: String? by mutableStateOf(null)

    /** Набранное в поле. Обычный `var`: это ввод человека, а не состояние. */
    var nameDraft: String by mutableStateOf("")

    /**
     * Пока файл не сохранён, имя — это просто строчка в кэше, и менять её
     * свободно. После сохранения копия уже разложена по месту: там имя правят
     * «Файлы», а в медиатеке его и не видно.
     */
    val canRename: Boolean
        get() = !isSaved && !isSaving

    val isRenaming: Boolean
        get() = renamedId != null

    /** Чем кончится имя, что бы ни набрали в поле. `null` — расширения нет. */
    val renameNote: String?
        get() {
            val extension = renamedResult?.outputName?.substringAfterLast(delimiter = '.', missingDelimiterValue = "")
            return extension?.takeIf { it.isNotEmpty() }?.let { resources.getString(R.string.rename_note, ".$it") }
        }

    fun rename(result: ProcessingResult) {
        if (!canRename) return

        renamedId = result.item.id
        nameDraft = result.outputName.substringBeforeLast(delimiter = '.', missingDelimiterValue = result.outputName)
    }

    /** Закрыть правку могут и мимо кнопки — нажатием по затемнению. */
    fun cancelRename() {
        renamedId = null
    }

    fun applyRename() {
        val target = renamedResult ?: return
        renamedId = null
        if (!canRename) return

        val extension = target.outputName.substringAfterLast(delimiter = '.', missingDelimiterValue = "")
        val name = fileName(nameDraft, extension)?.takeIf { it != target.outputName } ?: return

        scope.launch {
            try {
                val renamed = service.rename(target, name)
                outcome = outcome.copy(results = results.map { if (it.item.id == renamed.item.id) renamed else it })
                analytics.didRenameResult()
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                say(R.string.rename_failed)
            }
        }
    }

    private val renamedResult: ProcessingResult?
        get() = results.firstOrNull { it.item.id == renamedId }

    /**
     * Набранное превращаем в имя файла: разделители пути и точки по краям
     * увели бы файл из своей папки или спрятали бы его от системы, а длину
     * ограничиваем, потому что дальше имя всё равно обрежется в интерфейсе.
     * `null` — набрано пустое: тогда имя остаётся прежним.
     */
    private fun fileName(draft: String, extension: String): String? {
        val base = draft
            .map { if (it in FORBIDDEN || it.isISOControl()) ' ' else it }
            .joinToString(separator = "")
            .trim { it == '.' || it.isWhitespace() }
            .take(NAME_LIMIT)

        if (base.isEmpty()) return null
        return if (extension.isEmpty()) base else "$base.$extension"
    }

    // MARK: Удаление оригиналов

    /**
     * Пока результат не сохранён, оригинал — единственная копия: предлагать
     * его удалить в этот момент нельзя.
     */
    val showsDeleteOriginals: Boolean
        get() = copy.offersDeletingOriginals(outcome.kind) && !outcome.isEmpty && isSaved

    val deleteSubtitle: String
        get() = formatters.sizes.string(results.originalByteCount)

    fun deleteOriginals() {
        if (isDeleting || !isSaved) return
        isDeleting = true

        scope.launch {
            val request = library.deleteRequest(results.map { it.item })
            if (request == null) {
                // Система удалила без вопросов — спрашивать больше не о чем.
                deleteAnswered(confirmed = true)
            } else {
                deleteRequest = request
            }
        }
    }

    /** Экран показал системный запрос — второй раз запускать его не надо. */
    fun deleteLaunched() {
        deleteRequest = null
    }

    fun deleteAnswered(confirmed: Boolean) {
        isDeleting = false
        deleteRequest = null
        if (!confirmed) return

        analytics.didDeleteOriginals(results.size)
        router.popToRoot()
    }

    // MARK: Действия

    fun finish() {
        router.popToRoot()
    }

    private companion object {
        const val TOAST_MS = 2600L
        const val NAME_LIMIT = 60
        val FORBIDDEN = charArrayOf('/', '\\', ':')
    }
}
