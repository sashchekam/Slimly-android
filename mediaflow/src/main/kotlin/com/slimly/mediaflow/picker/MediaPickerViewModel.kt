package com.slimly.mediaflow.picker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.MediaAlbum
import com.slimly.core.model.MediaItem
import com.slimly.core.model.MediaKind
import com.slimly.core.model.MediaPage
import com.slimly.core.model.totalByteCount
import com.slimly.core.service.MediaAccess
import com.slimly.core.service.MediaLibrary
import com.slimly.mediaflow.FlowAnalytics
import com.slimly.mediaflow.FlowDestination
import com.slimly.mediaflow.MediaThumbnailLoader
import com.slimly.mediaflow.go
import com.slimly.mediaflow.thumbnailLoader
import com.slimly.navigation.Router
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

/** Правила шага выбора файлов для конкретного инструмента. */
data class MediaPickerConfig(
    val title: String,
    val hint: String,
    /** Чем ограничена выборка: конвертеру видео нечего показывать фото. */
    val kind: MediaKind?,
    /** Сколько файлов можно взять за раз — предел бесплатной обработки. */
    val limit: Int,
    val noun: CountNoun,
    /**
     * Показывать ли вес файлов. У скана он ни на что не влияет: страницу
     * выбирают по содержимому, а не по мегабайтам.
     */
    val showsSize: Boolean = true,
)

class MediaPickerViewModel(
    val config: MediaPickerConfig,
    private val library: MediaLibrary,
    private val formatters: SlimlyFormatters,
    private val analytics: FlowAnalytics,
    private val router: Router,
    private val scope: CoroutineScope,
    private val destination: (List<MediaItem>) -> FlowDestination,
) {
    var albums: List<MediaAlbum> by mutableStateOf(emptyList())
        private set

    var items: List<MediaItem> by mutableStateOf(emptyList())
        private set

    /**
     * Выбор в порядке нажатий: у скана этот порядок становится порядком
     * страниц, поэтому множеством его хранить нельзя.
     */
    var selection: List<MediaItem> by mutableStateOf(emptyList())
        private set

    var albumId: String by mutableStateOf(MediaAlbum.RECENT_ID)
        private set

    var access: MediaAccess by mutableStateOf(MediaAccess.NOT_DETERMINED)
        private set

    var isLoading: Boolean by mutableStateOf(true)
        private set

    /**
     * Догрузка следующей страницы: сетка уже видна, поэтому индикатор
     * у неё свой, под последним рядом.
     */
    var isLoadingMore: Boolean by mutableStateOf(false)
        private set

    var hasMore: Boolean by mutableStateOf(false)
        private set

    /**
     * Даты плиток, которые сейчас в кадре, — как в системной галерее.
     * Медиатека идёт от свежих файлов к старым, поэтому надпись читается
     * как «докуда долистали», и по ней видно, что листать дальше.
     *
     * `null` — датой не подписан ни один видимый файл: MediaStore её знает
     * не про всё, и выдумывать нечего.
     */
    fun visibleDates(indices: List<Int>): String? {
        var earliest: Instant? = null
        var latest: Instant? = null

        indices.forEach { index ->
            val date = items.getOrNull(index)?.creationDate ?: return@forEach
            earliest = earliest?.let { minOf(it, date) } ?: date
            latest = latest?.let { maxOf(it, date) } ?: date
        }

        val from = earliest ?: return null
        return formatters.dates.string(from, latest ?: from)
    }

    /** Системный запрос показываем один раз за посещение экрана. */
    var hasRequestedAccess: Boolean by mutableStateOf(false)
        private set

    /** Докуда дочитали. Считается страницами, а не длиной витрины: повторы из неё выбрасываются. */
    private var offset = 0
    private var shown = mutableSetOf<String>()
    private var pageJob: Job? = null

    val thumbnails: MediaThumbnailLoader = library.thumbnailLoader

    /** Разрешения, которые экран попросит у системы. */
    val permissions: List<String> = library.permissions(config.kind)

    // MARK: Загрузка

    /**
     * Системный запрос доступа показываем не на главной, а здесь: спрашивать
     * про галерею имеет смысл в тот момент, когда пользователь пришёл выбирать
     * файлы, — тогда понятно, зачем.
     */
    suspend fun load() {
        access = library.access()
        if (!access.allowsBrowsing) {
            isLoading = false
            return
        }
        analytics.setAccess(access)
        reload()
    }

    /** Экран показал системный запрос — второй раз показывать его незачем. */
    fun accessRequested() {
        hasRequestedAccess = true
    }

    /**
     * Система ответила на запрос.
     *
     * «Ещё не спрашивали» после ответа означает отказ: другого способа
     * отличить одно от другого у сервиса нет, а здесь ответ виден напрямую.
     */
    fun accessAnswered() {
        scope.launch {
            val granted = library.access()
            access = if (granted.allowsBrowsing) granted else MediaAccess.DENIED
            analytics.didAnswerAccess(access)
            analytics.setAccess(access)
            if (access.allowsBrowsing) reload() else isLoading = false
        }
    }

    fun select(album: MediaAlbum) {
        if (album.id == albumId) return
        albumId = album.id
        isLoading = true
        // Выбор не сбрасываем: альбом — это фильтр витрины, а не новая сессия.
        reset()
        loadPage(album.id, offset = 0, appending = false)
    }

    /**
     * Сетка сообщает, что показывает последние плитки. Продолжение просим
     * не по кнопке и не в самом низу: пользователь листает медиатеку
     * непрерывно, и пауза посреди прокрутки читалась бы как конец списка.
     */
    fun loadMoreIfNeeded() {
        if (!hasMore || isLoadingMore) return
        isLoadingMore = true
        loadPage(albumId, offset = offset, appending = true)
    }

    private fun reload() {
        isLoading = true
        reset()
        scope.launch { albums = library.albums(config.kind) }
        loadPage(albumId, offset = 0, appending = false)
    }

    private fun loadPage(album: String, offset: Int, appending: Boolean) {
        pageJob?.cancel()
        pageJob = scope.launch {
            val page = library.items(album, config.kind, offset, PAGE_SIZE)
            // Пока читался альбом, пользователь мог переключиться на другой:
            // тогда витрину заполняет уже та, более поздняя загрузка.
            if (albumId != album) return@launch
            apply(page, appending)
            isLoading = false
            isLoadingMore = false
        }
    }

    private fun apply(page: MediaPage, appending: Boolean) {
        if (!appending) {
            shown = mutableSetOf()
            offset = 0
        }
        // Медиатека могла измениться между страницами — тогда окно сдвинется
        // и файл придёт вторым экземпляром. Для списка это одинаковые ключи,
        // поэтому повторы отсекаем здесь.
        val fresh = page.items.filter { shown.add(it.id) }
        items = if (appending) items + fresh else fresh
        offset += page.items.size
        hasMore = page.hasMore
    }

    private fun reset() {
        pageJob?.cancel()
        pageJob = null
        items = emptyList()
        shown = mutableSetOf()
        offset = 0
        hasMore = false
        isLoadingMore = false
    }

    // MARK: Состояние экрана

    val isDenied: Boolean
        get() = access == MediaAccess.DENIED

    /**
     * Доступ есть, а показывать нечего: у инструмента своя выборка,
     * и в медиатеке может просто не быть видео.
     */
    val isEmpty: Boolean
        get() = !isLoading && access.allowsBrowsing && items.isEmpty()

    /**
     * При частичном доступе список — это ровно то, что пользователь отобрал
     * сам, поэтому «пусто» здесь значит другое.
     */
    val isLimited: Boolean
        get() = access == MediaAccess.LIMITED

    val currentAlbum: MediaAlbum?
        get() = albums.firstOrNull { it.id == albumId }

    // MARK: Выбор

    fun isSelected(item: MediaItem): Boolean = selection.any { it.id == item.id }

    /**
     * Плитка сверх лимита не гаснет молча: она приглушена и не реагирует,
     * поэтому предел виден до нажатия, а не после отказа.
     */
    fun isBlocked(item: MediaItem): Boolean = !isSelected(item) && isAtLimit

    fun toggle(item: MediaItem) {
        val existing = selection.indexOfFirst { it.id == item.id }
        selection = when {
            existing >= 0 -> selection.filterIndexed { index, _ -> index != existing }
            isAtLimit -> selection
            else -> selection + item
        }
    }

    val isAtLimit: Boolean
        get() = selection.size >= config.limit

    val hasSelection: Boolean
        get() = selection.isNotEmpty()

    // MARK: Показатели

    val limitFraction: Float
        get() = (selection.size.toFloat() / config.limit).coerceAtMost(1f)

    val selectionText: String
        get() = formatters.counts.string(selection.size, config.noun)

    val selectionSizeText: String
        get() = formatters.sizes.string(selection.totalByteCount)

    // MARK: Навигация

    fun goBack() {
        router.pop()
    }

    fun proceed() {
        if (!hasSelection) return
        analytics.didPickFiles(selection)
        router.go(destination(selection))
    }

    private companion object {
        /**
         * Сколько файлов читаем за раз. Вес каждого система отдаёт поштучно,
         * поэтому окно небольшое: экран открывается сразу, а остальное
         * подтягивается по мере прокрутки.
         */
        const val PAGE_SIZE = 150
    }
}
