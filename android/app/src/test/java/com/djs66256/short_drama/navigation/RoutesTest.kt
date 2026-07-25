package com.djs66256.short_drama.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `home route is home`() {
        assertEquals("home", AppDestination.Route.HOME)
    }

    @Test
    fun `play route generates correct path`() {
        assertEquals("play/abc123", AppDestination.play("abc123"))
    }

    @Test
    fun `player alias route generates correct path`() {
        assertEquals("player/abc123", AppDestination.playerAlias("abc123"))
    }

    @Test
    fun `detail route generates correct path`() {
        assertEquals("detail/xyz456", AppDestination.detail("xyz456"))
    }
}
