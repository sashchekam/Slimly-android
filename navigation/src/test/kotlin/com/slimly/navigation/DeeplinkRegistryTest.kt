package com.slimly.navigation

import com.slimly.core.deeplink.DeeplinkUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeeplinkRegistryTest {
    private val home = DeeplinkMapper { deeplink ->
        DeeplinkDestination(Screen.Home).takeIf { deeplink.host == "home" }
    }
    private val tool = DeeplinkMapper { deeplink ->
        DeeplinkDestination(routes = listOf(Screen.Home, Screen.Details), sheet = Screen.Preview)
            .takeIf { deeplink.host == "tool" }
    }

    @Test
    fun `выигрывает первый подошедший мэппер`() {
        val registry = DeeplinkRegistry(listOf(tool, home, DeeplinkMapper { DeeplinkDestination(Screen.Preview) }))

        assertEquals(listOf(Screen.Home), registry.destination(url("home"))?.routes)
    }

    @Test
    fun `пустой результат не считается ответом`() {
        val empty = DeeplinkMapper { DeeplinkDestination(routes = emptyList()) }
        val registry = DeeplinkRegistry(listOf(empty, home))

        assertEquals(listOf(Screen.Home), registry.destination(url("home"))?.routes)
    }

    @Test
    fun `ссылка без своего мэппера остаётся без адреса`() {
        val registry = DeeplinkRegistry(listOf(home, tool))

        assertNull(registry.destination(url("unknown")))
    }

    @Test
    fun `ссылка раскладывается в стек и шторку поверх него`() {
        val router = Router(Screen.Home)
        val registry = DeeplinkRegistry(listOf(home, tool))

        router.navigate(requireNotNull(registry.destination(url("tool"))))

        assertEquals(Screen.Home, router.root)
        assertEquals(listOf(Screen.Details), router.path)
        assertEquals(Screen.Preview, router.sheet)
    }

    private fun url(host: String) = DeeplinkUrl(host = host, pathSegments = emptyList(), queryItems = emptyMap())
}
