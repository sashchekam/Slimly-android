package com.slimly.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterTest {
    private val router = Router(Screen.Home)

    @Test
    fun `push кладёт маршрут поверх корня`() {
        router.push(Screen.Details)

        assertEquals(listOf(Screen.Details), router.path)
        assertEquals(listOf(Screen.Home, Screen.Details), router.screens)
        assertTrue(router.canPop)
    }

    @Test
    fun `pop снимает верхний маршрут, но не трогает корень`() {
        router.push(listOf(Screen.Details, Screen.Settings))

        router.pop()

        assertEquals(listOf(Screen.Details), router.path)

        router.pop()
        router.pop()

        assertEquals(Screen.Home, router.root)
        assertTrue(router.path.isEmpty())
    }

    @Test
    fun `смена корня сбрасывает стек`() {
        router.push(Screen.Details)

        router.changeRoot(Screen.Settings)

        assertEquals(Screen.Settings, router.root)
        assertTrue(router.path.isEmpty())
    }

    @Test
    fun `replace раскладывает первый маршрут в корень, остальные в стек`() {
        router.present(Screen.Preview)

        router.replace(listOf(Screen.Settings, Screen.Details))

        assertEquals(Screen.Settings, router.root)
        assertEquals(listOf(Screen.Details), router.path)
        // Модалки закрываются: новый «мир» не наследует шторку от старого.
        assertNull(router.sheet)
    }

    @Test
    fun `replace пустым списком ничего не меняет`() {
        router.push(Screen.Details)

        router.replace(emptyList())

        assertEquals(Screen.Home, router.root)
        assertEquals(listOf(Screen.Details), router.path)
    }

    @Test
    fun `show открывает маршрут выбранным способом`() {
        router.show(Screen.Details, RoutePresentation.PUSH)
        router.show(Screen.Settings, RoutePresentation.SHEET)
        router.show(Screen.Preview, RoutePresentation.COVER)

        assertEquals(listOf(Screen.Details), router.path)
        assertEquals(Screen.Settings, router.sheet)
        assertEquals(Screen.Preview, router.cover)
        assertTrue(router.hasModal)
    }

    @Test
    fun `дочерний роутер гасит свой слот у родителя, а не соседний`() {
        router.present(Screen.Settings)
        router.cover(Screen.Preview)
        val sheetRouter = Router(Screen.Settings, parent = router)

        sheetRouter.dismiss()

        assertNull(router.sheet)
        assertEquals(Screen.Preview, router.cover)
    }

    @Test
    fun `dismiss корневого роутера ничего не делает`() {
        router.push(Screen.Details)

        router.dismiss()

        assertEquals(listOf(Screen.Details), router.path)
    }

    @Test
    fun `dismissToRoot сворачивает всю цепочку слоёв`() {
        router.push(Screen.Details)
        router.present(Screen.Settings)
        val sheetRouter = Router(Screen.Settings, parent = router)
        sheetRouter.push(Screen.Preview)
        sheetRouter.cover(Screen.Preview)
        val coverRouter = Router(Screen.Preview, parent = sheetRouter)

        coverRouter.dismissToRoot()

        assertNull(router.sheet)
        assertNull(router.cover)
        assertTrue(router.path.isEmpty())
        assertEquals(Screen.Home, router.root)
    }

    @Test
    fun `outermost находит самый внешний роутер цепочки`() {
        val sheetRouter = Router(Screen.Settings, parent = router)
        val coverRouter = Router(Screen.Preview, parent = sheetRouter)

        assertSame(router, coverRouter.outermost)
        assertSame(router, router.outermost)
    }

    @Test
    fun `restart меняет корень самого внешнего слоя и закрывает модалки`() {
        router.present(Screen.Settings)
        val sheetRouter = Router(Screen.Settings, parent = router)

        sheetRouter.restart(Screen.Details)

        assertEquals(Screen.Details, router.root)
        assertNull(router.sheet)
        assertTrue(router.path.isEmpty())
    }

    @Test
    fun `changePath задаёт стек целиком, не трогая корень`() {
        router.changePath(listOf(Screen.Details, Screen.Settings))

        assertEquals(Screen.Home, router.root)
        assertEquals(listOf(Screen.Details, Screen.Settings), router.path)
    }
}
