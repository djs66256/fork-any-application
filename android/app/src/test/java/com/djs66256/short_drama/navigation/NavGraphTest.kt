package com.djs66256.short_drama.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavGraphTest {

    @Test
    fun `T-08 canonical and alias player routes hide bottom bar`() {
        assertFalse(shouldShowBottomBar(AppDestination.Route.PLAY))
        assertFalse(shouldShowBottomBar(AppDestination.Route.PLAYER_ALIAS))
    }

    @Test
    fun `T-08 alias route remains part of player route set`() {
        assertTrue(AppDestination.isPlayerRoute(AppDestination.Route.PLAYER_ALIAS))
        assertTrue(AppDestination.isPlayerRoute(AppDestination.Route.PLAY))
    }

    @Test
    fun `T-08 home and detail routes keep bottom bar visible`() {
        assertTrue(shouldShowBottomBar(AppDestination.Route.HOME))
        assertTrue(shouldShowBottomBar(AppDestination.Route.DETAIL))
        assertTrue(shouldShowBottomBar(null))
    }
}
