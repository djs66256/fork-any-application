package com.djs66256.short_drama.feature.menu.model

import com.djs66256.short_drama.navigation.PendingRoute

enum class MenuPanelSection {
    LOGIN_HEADER,
    MESSAGE_PREVIEW,
    RECENTLY_VIEWED,
    GAME_CENTER,
    COMMON_FUNCTIONS,
}

sealed interface MenuPanelStaticAction {
    data class Navigate(val pendingRoute: PendingRoute) : MenuPanelStaticAction

    data class Feedback(val message: String) : MenuPanelStaticAction
}

data class MenuLoginHeaderEntry(
    val title: String,
    val subtitle: String,
    val action: MenuPanelStaticAction.Navigate,
)

data class MenuMessagePreviewEntry(
    val title: String,
    val summary: String,
    val action: MenuPanelStaticAction.Navigate,
)

data class MenuGameCenterEntry(
    val id: String,
    val label: String,
    val action: MenuPanelStaticAction.Feedback,
)

data class MenuCommonFunctionEntry(
    val title: String,
    val subtitle: String,
    val action: MenuPanelStaticAction.Navigate,
)

data class MenuPanelStaticContent(
    val loginHeader: MenuLoginHeaderEntry,
    val messagePreview: MenuMessagePreviewEntry,
    val gameCenterEntries: List<MenuGameCenterEntry>,
    val commonFunctionEntries: List<MenuCommonFunctionEntry>,
)

object MenuPanelStaticEntries {
    const val MAX_RECENTLY_VIEWED_COUNT = 3
    const val COMING_SOON_MESSAGE = "即将上线"

    val sectionOrder: List<MenuPanelSection> = listOf(
        MenuPanelSection.LOGIN_HEADER,
        MenuPanelSection.MESSAGE_PREVIEW,
        MenuPanelSection.RECENTLY_VIEWED,
        MenuPanelSection.GAME_CENTER,
        MenuPanelSection.COMMON_FUNCTIONS,
    )

    val content: MenuPanelStaticContent = MenuPanelStaticContent(
        loginHeader = MenuLoginHeaderEntry(
            title = "登录后同步你的观看权益",
            subtitle = "登录后可查看消息、预约提醒和下载内容。",
            action = MenuPanelStaticAction.Navigate(PendingRoute.MenuLogin),
        ),
        messagePreview = MenuMessagePreviewEntry(
            title = "我的消息",
            summary = "系统通知、活动提醒与互动消息都会在这里汇总。",
            action = MenuPanelStaticAction.Navigate(PendingRoute.MenuMessages),
        ),
        gameCenterEntries = listOf(
            MenuGameCenterEntry(
                id = "spin",
                label = "幸运转盘",
                action = MenuPanelStaticAction.Feedback(COMING_SOON_MESSAGE),
            ),
            MenuGameCenterEntry(
                id = "match",
                label = "消消乐",
                action = MenuPanelStaticAction.Feedback(COMING_SOON_MESSAGE),
            ),
            MenuGameCenterEntry(
                id = "draw",
                label = "抽奖机",
                action = MenuPanelStaticAction.Feedback(COMING_SOON_MESSAGE),
            ),
            MenuGameCenterEntry(
                id = "flip",
                label = "翻牌挑战",
                action = MenuPanelStaticAction.Feedback(COMING_SOON_MESSAGE),
            ),
        ),
        commonFunctionEntries = listOf(
            MenuCommonFunctionEntry(
                title = "我的预约",
                subtitle = "查看已预约的新剧与提醒。",
                action = MenuPanelStaticAction.Navigate(PendingRoute.MenuBooking),
            ),
            MenuCommonFunctionEntry(
                title = "我的下载",
                subtitle = "离线缓存与下载管理能力建设中。",
                action = MenuPanelStaticAction.Navigate(PendingRoute.MenuDownloads),
            ),
        ),
    )
}
