package com.slimly.navigation

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.parcelize.Parcelize

/**
 * Состояние навигации одного слоя презентации.
 *
 * Один [Router] = один стек. Каждая модалка получает **собственный** дочерний
 * роутер, поэтому иерархия роутеров повторяет иерархию презентаций:
 * «стек внутри шторки» и «модалка поверх модалки» работают без спецслучаев,
 * а [dismissToRoot] доступен с любой глубины.
 *
 * Состояние наружу только на чтение: менять его — значит звать навигацию
 * по имени, а не присваивать поля.
 */
class Router(root: Route, val parent: Router? = null) {
    /** Корень стека. Меняется, когда меняется «мир»: тур → главный экран. */
    var root: Route by mutableStateOf(root)
        private set

    /** Всё, что лежит поверх корня. */
    var path: List<Route> by mutableStateOf(emptyList())
        private set

    var sheet: Route? by mutableStateOf(null)
        private set

    /**
     * Экран поверх всего — для того, у чего своя полноэкранная жизнь:
     * камера, системный просмотрщик документа.
     */
    var cover: Route? by mutableStateOf(null)
        private set

    /**
     * Маршрут, которым этот слой был показан у родителя.
     * Нужен, чтобы [dismiss] погасил именно свой слот, а не соседний.
     */
    private val presentedRoute: Route? = if (parent == null) null else root

    /** Корень и всё, что лежит поверх него, — ровно то, что рисует хост. */
    val screens: List<Route>
        get() = listOf(root) + path

    val canPop: Boolean
        get() = path.isNotEmpty()

    /** Поверх этого слоя показана модалка: системный «назад» принадлежит ей. */
    val hasModal: Boolean
        get() = sheet != null || cover != null

    /**
     * Самый внешний роутер цепочки — тот, поверх которого показаны все
     * модалки. На iOS он назван `topmost`.
     */
    val outermost: Router
        get() = parent?.outermost ?: this

    // MARK: Навигация внутри слоя

    fun push(route: Route) {
        path = path + route
    }

    fun push(routes: List<Route>) {
        path = path + routes
    }

    fun pop() {
        path = path.dropLast(1)
    }

    fun popToRoot() {
        path = emptyList()
    }

    /** Задать стек поверх текущего корня целиком (ссылка внутрь одного «мира»). */
    fun changePath(routes: List<Route>) {
        path = routes
    }

    /**
     * Сменить корень слоя, стек при этом сбрасывается.
     *
     * Не `setRoot`, как на iOS: у свойства [root] уже есть свой сеттер,
     * и на JVM два `setRoot` не уживаются.
     */
    fun changeRoot(route: Route) {
        root = route
        path = emptyList()
    }

    /**
     * Полностью заменить содержимое слоя: первый маршрут становится корнем,
     * остальные ложатся в стек. Основной приём для ссылок.
     */
    fun replace(routes: List<Route>) {
        val first = routes.firstOrNull() ?: return
        dismissModals()
        root = first
        path = routes.drop(1)
    }

    // MARK: Модалки

    fun present(route: Route) {
        sheet = route
    }

    fun cover(route: Route) {
        cover = route
    }

    fun dismissSheet() {
        sheet = null
    }

    fun dismissCover() {
        cover = null
    }

    /** Закрыть модалки, показанные из этого слоя. */
    fun dismissModals() {
        sheet = null
        cover = null
    }

    /** Закрыть сам этот слой у родителя. Для корневого роутера — пустая операция. */
    fun dismiss() {
        val parent = parent ?: return
        val route = presentedRoute ?: return
        when (route) {
            parent.sheet -> parent.sheet = null
            parent.cover -> parent.cover = null
        }
    }

    /**
     * Свернуть всю навигацию до корня приложения: закрыть модалки вверх
     * по цепочке и сбросить стек самого внешнего роутера.
     */
    fun dismissToRoot() {
        var current: Router = this
        while (current.parent != null) {
            current.dismiss()
            current = requireNotNull(current.parent)
        }
        current.dismissModals()
        current.popToRoot()
    }

    /** Перезапуск навигации с новым корнем — конец тура, смена «мира». */
    fun restart(route: Route) {
        val target = outermost
        dismissToRoot()
        target.changeRoot(route)
    }

    companion object {
        /**
         * Хранитель состояния роутера. Процесс убивают — стек, модалки
         * и корень переживают это и приезжают обратно из `savedInstanceState`.
         *
         * Родитель не сохраняется: цепочку слоёв заново собирает хост,
         * разворачивая модалки сверху вниз.
         */
        fun saver(parent: Router? = null): Saver<Router, RouterState> = Saver(
            save = { RouterState(root = it.root, path = it.path, sheet = it.sheet, cover = it.cover) },
            restore = { state ->
                Router(state.root, parent).also {
                    it.path = state.path
                    it.sheet = state.sheet
                    it.cover = state.cover
                }
            },
        )
    }
}

/**
 * Сохранённое состояние слоя. Публичный, а не приватный, потому что его имя
 * стоит в типе [Router.saver], а `Parcelable` вдобавок читается системой
 * по имени класса.
 */
@Parcelize
data class RouterState(val root: Route, val path: List<Route>, val sheet: Route?, val cover: Route?) : Parcelable

/**
 * Роутер слоя, переживающий и перерисовку, и смерть процесса.
 *
 * Корень — только начальный: дальше его меняет [Router.setRoot], и заново
 * из параметра роутер не собирается.
 */
@Composable
fun rememberRouter(root: Route, parent: Router? = null): Router =
    rememberSaveable(saver = Router.saver(parent)) { Router(root, parent) }
