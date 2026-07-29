package com.djs66256.short_drama.feature.menu.model

import com.djs66256.short_drama.navigation.PendingRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuPanelStaticEntriesTest {

    @Test
    fun `T-07 static section order remains stable`() {
        assertEquals(
            listOf(
                MenuPanelSection.LOGIN_HEADER,
                MenuPanelSection.MESSAGE_PREVIEW,
                MenuPanelSection.RECENTLY_VIEWED,
                MenuPanelSection.GAME_CENTER,
                MenuPanelSection.COMMON_FUNCTIONS,
            ),
            MenuPanelStaticEntries.sectionOrder,
        )
    }

    @Test
    fun `T-07 login and message entries navigate to placeholder routes`() {
        val content = MenuPanelStaticEntries.content

        assertEquals(PendingRoute.MenuLogin, content.loginHeader.action.pendingRoute)
        assertEquals(PendingRoute.MenuMessages, content.messagePreview.action.pendingRoute)
        assertEquals("系统通知、活动提醒与互动消息都会在这里汇总。", content.messagePreview.defaultSummary)
    }

    @Test
    fun `T-07 game center entries emit feedback only`() {
        val actions = MenuPanelStaticEntries.content.gameCenterEntries.map { it.action }

        assertEquals(4, actions.size)
        assertTrue(actions.all { it is MenuPanelStaticAction.Feedback })
        assertTrue(actions.all { it.message == MenuPanelStaticEntries.COMING_SOON_MESSAGE })
    }

    @Test
    fun `T-07 common function entries navigate to booking and downloads`() {
        val actions = MenuPanelStaticEntries.content.commonFunctionEntries.map { it.action.pendingRoute }

        assertEquals(
            listOf(PendingRoute.MenuBooking, PendingRoute.MenuDownloads),
            actions,
        )
    }
}
