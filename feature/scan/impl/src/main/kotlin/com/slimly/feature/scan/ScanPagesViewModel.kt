package com.slimly.feature.scan

import android.content.res.Resources
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.slimly.core.format.CountNoun
import com.slimly.core.format.SlimlyFormatters
import com.slimly.core.model.DocumentQuad
import com.slimly.core.model.MediaItem
import com.slimly.core.model.QuadPoint
import com.slimly.core.model.ScanBounds
import com.slimly.core.service.DocumentBoundsDetector
import com.slimly.feature.scan.api.ScanRoute
import com.slimly.mediaflow.MediaThumbnailLoader
import com.slimly.navigation.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Шаг «границы страниц» — единственное место, где человек может не согласиться
 * с автоматикой.
 *
 * Границы могла бы искать и сама обработка, уже во время сборки, — но тогда
 * рамку никто не видит, и косой кадр обнаруживается только в готовом PDF.
 * Здесь то же самое происходит до обработки и на виду: согласиться можно
 * одним «Далее», а поправить углы — у любой страницы.
 */
class ScanPagesViewModel(
    items: List<MediaItem>,
    private val resources: Resources,
    private val formatters: SlimlyFormatters,
    private val detector: DocumentBoundsDetector,
    val thumbnails: MediaThumbnailLoader,
    private val analytics: ScanAnalytics,
    private val router: Router,
) {
    /**
     * Страница со своими границами.
     *
     * Кадр держим растром: разбор и показ — дело экрана, а модель о нём
     * ничего не знает.
     */
    data class Page(
        val item: MediaItem,
        val frame: Bitmap? = null,
        /** Что нашла автоматика. `null` — листа в кадре не видно. */
        val detected: DocumentQuad? = null,
        /** Рамка, которая сейчас на экране. */
        val quad: DocumentQuad = DocumentQuad.FULL,
        /** Углы двигали руками — «Авто» вернёт найденное. */
        val isManual: Boolean = false,
        /** Кадр загружен и границы уже искали. */
        val isReady: Boolean = false,
    ) {
        val id: String
            get() = item.id
    }

    var pages: List<Page> by mutableStateOf(items.map { Page(it) })
        private set

    /** Какая страница открыта крупно. */
    var index: Int by mutableStateOf(0)
        private set

    // MARK: Загрузка

    /**
     * Кадры и границы идут по очереди, а не разом: страниц не больше пяти,
     * зато каждая — полный обход пикселей, и параллельно они только отнимали
     * бы друг у друга процессор.
     */
    suspend fun load() {
        pages.indices.forEach { position ->
            val page = pages.getOrNull(position) ?: return@forEach
            if (page.isReady) return@forEach

            val item = page.item
            val frame = thumbnails.thumbnail(item, PREVIEW_SIDE)
            val detected = frame?.let { withContext(Dispatchers.Default) { detector.bounds(it) } }

            // Пока грузили, страницу могли и убрать — ищем её заново.
            update(item.id) {
                it.copy(
                    frame = frame,
                    detected = detected,
                    // Ничего не нашли — рамка по краю кадра: она честно
                    // говорит, что обрезать нечего, и её сразу можно
                    // потянуть за угол.
                    quad = if (it.isManual) it.quad else detected ?: DocumentQuad.FULL,
                    isReady = true,
                )
            }
        }
    }

    // MARK: Состояние

    val page: Page
        get() = pages[index]

    val hasMultiplePages: Boolean
        get() = pages.size > 1

    fun select(position: Int) {
        if (position in pages.indices) index = position
    }

    fun isCurrent(page: Page): Boolean = page.id == this.page.id

    // MARK: Правка

    /**
     * Перетаскивание угла. Рамку, сложившуюся бантиком, не принимаем —
     * точка просто не доезжает, а палец продолжает вести.
     */
    fun move(corner: DocumentQuad.Corner, to: QuadPoint) {
        val moved = page.quad.moving(corner, to) ?: return
        update(page.id) { it.copy(quad = moved, isManual = true) }
    }

    /** Вернуть найденное автоматикой. */
    fun resetToDetected() {
        val detected = page.detected ?: return
        update(page.id) { it.copy(quad = detected, isManual = false) }
    }

    /** Взять кадр целиком — для страницы, которую обрезать не нужно. */
    fun useWholeFrame() {
        update(page.id) { it.copy(quad = DocumentQuad.FULL, isManual = true) }
    }

    val canResetToDetected: Boolean
        get() = page.detected?.let { it != page.quad } == true

    val canUseWholeFrame: Boolean
        get() = page.quad != DocumentQuad.FULL

    // MARK: Подписи

    val title: String
        get() = resources.getString(R.string.pages_title)

    /** «Страница 2 из 3» — или просто состояние, когда страница одна. */
    val subtitle: String
        get() = if (hasMultiplePages) {
            resources.getString(R.string.pages_subtitle, index + 1, pages.size, status)
        } else {
            status
        }

    private val status: String
        get() = resources.getString(
            when {
                !page.isReady -> R.string.status_searching
                page.isManual -> R.string.status_adjusted
                page.detected == null -> R.string.status_not_found
                else -> R.string.status_found
            },
        )

    /**
     * Подсказка под кадром. Говорит про рамку, а не про кнопки: человек
     * смотрит на страницу, а не на интерфейс.
     */
    val hint: String
        get() = resources.getString(
            when {
                !page.isReady -> R.string.pages_searching
                page.detected == null && !page.isManual -> R.string.pages_not_found
                else -> R.string.pages_adjust_hint
            },
        )

    val ctaTitle: String
        get() = resources.getString(
            R.string.pages_action,
            formatters.counts.string(pages.size, CountNoun.PAGE),
        )

    // MARK: Навигация

    fun goBack() {
        router.pop()
    }

    /**
     * Дальше идёт общий шаг формата, а границы едут с ним: обработке нужны
     * именно те рамки, что человек видел.
     *
     * В таблицу попадают только показанные границы. Страница, у которой
     * автоматика ничего не нашла и которую не трогали руками, идёт без них —
     * пусть обработка попробует найти лист сама, в полном разрешении.
     */
    fun proceed() {
        val bounds = pages
            .filter { it.isReady && (it.isManual || it.detected != null) }
            .fold(ScanBounds.AUTOMATIC) { table, page -> table.with(page.id, page.quad) }

        // Правки и промахи автоматики считаем здесь, а не в обработке:
        // это оценка человеком того, что она нашла, и другого места,
        // где она видна, у нас нет.
        analytics.didConfirmBounds(
            count = pages.size,
            edited = pages.count { it.isManual },
            missed = pages.count { it.isReady && it.detected == null },
        )
        router.push(ScanRoute.Format(items = pages.map { it.item }, bounds = bounds))
    }

    private fun update(id: String, change: (Page) -> Page) {
        pages = pages.map { if (it.id == id) change(it) else it }
    }

    private companion object {
        /**
         * Сторона кадра, который просим у медиатеки. Границы ищем в нём же,
         * а не в оригинале: рамка должна обещать ровно то, что видно
         * на экране.
         */
        const val PREVIEW_SIDE = 1400
    }
}
