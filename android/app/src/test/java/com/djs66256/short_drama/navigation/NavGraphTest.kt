package com.djs66256.short_drama.navigation

import com.djs66256.short_drama.feature.menu.ui.shouldHandleMenuBack
import com.djs66256.short_drama.feature.menu.ui.shouldRenderMenuDrawer
import org.junit.Assert.assertEquals
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

    @Test
    fun `T-08 menu placeholder specs register all placeholder routes`() {
        val specs = menuPlaceholderSpecs()

        assertEquals(
            listOf(
                AppDestination.menuLogin(),
                AppDestination.menuMessages(),
                AppDestination.menuBooking(),
                AppDestination.menuDownloads(),
            ),
            specs.map { it.route },
        )
        assertEquals(
            listOf("登录", "我的消息", "我的预约", "我的下载"),
            specs.map { it.title },
        )
        assertTrue(specs.all { it.description.contains("Native 承接页") })
    }

    @Test
    fun `T-08 open and closing menu states consume back before pop route`() {
        assertTrue(shouldHandleMenuBack(MenuPanelPresentationState.OPENING))
        assertTrue(shouldHandleMenuBack(MenuPanelPresentationState.OPEN))
        assertTrue(shouldHandleMenuBack(MenuPanelPresentationState.CLOSING))
        assertFalse(shouldHandleMenuBack(MenuPanelPresentationState.CLOSED))
    }

    @Test
    fun `T-06 mall placeholder is replaced by mall screen and login route`() {
        assertEquals(AppDestination.Route.MALL_LOGIN, "mall/login?productId={productId}&returnTarget={returnTarget}")
        assertFalse(menuPlaceholderSpecs().any { it.route == AppDestination.Route.MALL })
    }

    @Test
    fun `T-08 drawer renders while animating and hides after fully closed`() {
        assertTrue(shouldRenderMenuDrawer(MenuPanelPresentationState.OPEN, progress = 1f))
        assertTrue(shouldRenderMenuDrawer(MenuPanelPresentationState.CLOSING, progress = 0.3f))
        assertFalse(shouldRenderMenuDrawer(MenuPanelPresentationState.CLOSED, progress = 0f))
    }
}
