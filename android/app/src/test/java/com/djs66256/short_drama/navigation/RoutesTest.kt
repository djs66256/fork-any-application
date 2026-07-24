package com.djs66256.short_drama.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `T-07 HOME route is "home"`() {
        assertEquals("home", Routes.HOME)
    }

    @Test
    fun `T-07 player route generates correct path`() {
        assertEquals("player/abc123", Routes.player("abc123"))
    }

    @Test
    fun `T-07 dramaDetail route generates correct path`() {
        assertEquals("dramaDetail/xyz456", Routes.dramaDetail("xyz456"))
    }
}
