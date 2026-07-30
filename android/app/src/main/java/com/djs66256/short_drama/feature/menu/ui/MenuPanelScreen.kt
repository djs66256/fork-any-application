package com.djs66256.short_drama.feature.menu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.feature.menu.model.MenuCommonFunctionEntry
import com.djs66256.short_drama.feature.menu.model.MenuGameCenterEntry
import com.djs66256.short_drama.feature.menu.model.MenuPanelStaticEntries
import com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelEvent
import com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelViewModel
import com.djs66256.short_drama.navigation.PendingRoute
import kotlinx.coroutines.flow.collect

private const val DRAWER_REFERENCE_WIDTH = 842f
private const val DRAWER_REFERENCE_HEIGHT = 2400f
private const val MENU_OPEN_HEADER_CTA_X = 627f
private const val MENU_OPEN_HEADER_CTA_Y = 126f
private const val MENU_OPEN_HEADER_CTA_WIDTH = 190f
private const val MENU_OPEN_HEADER_CTA_HEIGHT = 74f

@Composable
fun MenuPanelRoute(
    onNavigateFromMenu: (PendingRoute) -> Unit,
    onShowFeedback: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MenuPanelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MenuPanelEvent.OpenPlayback -> {
                    onNavigateFromMenu(PendingRoute.Play(event.dramaId))
                }
            }
        }
    }

    MenuPanelScreen(
        uiState = uiState,
        onLoginClick = {
            onNavigateFromMenu(MenuPanelStaticEntries.content.loginHeader.action.pendingRoute)
        },
        onMessageClick = {
            onNavigateFromMenu(MenuPanelStaticEntries.content.messagePreview.action.pendingRoute)
        },
        onRetryRecentlyViewed = viewModel::retry,
        onRecentlyViewedClick = { item ->
            viewModel.onRecentlyViewedClick(item.dramaId)
        },
        onGameEntryClick = { entry ->
            onShowFeedback(entry.action.message)
        },
        onCommonFunctionClick = { entry ->
            onNavigateFromMenu(entry.action.pendingRoute)
        },
        modifier = modifier,
    )
}

@Composable
fun MenuPanelScreen(
    uiState: com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelUiState,
    onLoginClick: () -> Unit,
    onMessageClick: () -> Unit,
    onRetryRecentlyViewed: () -> Unit,
    onRecentlyViewedClick: (com.djs66256.short_drama.domain.model.RecentlyViewed) -> Unit,
    onGameEntryClick: (MenuGameCenterEntry) -> Unit,
    onCommonFunctionClick: (MenuCommonFunctionEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val staticContent = MenuPanelStaticEntries.content
    val recentItems = uiState.recentlyViewed.items.ifEmpty {
        listOf(
            com.djs66256.short_drama.domain.model.RecentlyViewed(
                dramaId = "fallback-1",
                title = "村里的吃人鬼",
                coverUrl = null,
                episodeNumber = 1,
                progress = 0.0,
                updatedAt = "",
            ),
            com.djs66256.short_drama.domain.model.RecentlyViewed(
                dramaId = "fallback-2",
                title = "一剑挽仙洲",
                coverUrl = null,
                episodeNumber = 119,
                progress = 0.0,
                updatedAt = "",
            ),
            com.djs66256.short_drama.domain.model.RecentlyViewed(
                dramaId = "fallback-3",
                title = "兼职帝君",
                coverUrl = null,
                episodeNumber = 1,
                progress = 0.0,
                updatedAt = "",
            ),
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        DrawerClickZone(
            x = MENU_OPEN_HEADER_CTA_X,
            y = MENU_OPEN_HEADER_CTA_Y,
            width = MENU_OPEN_HEADER_CTA_WIDTH,
            height = MENU_OPEN_HEADER_CTA_HEIGHT,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            onClick = onLoginClick,
        )

        DrawerClickZone(
            x = 46f,
            y = 289f,
            width = 148f,
            height = 238f,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            onClick = { onRecentlyViewedClick(recentItems[0]) },
        )
        DrawerClickZone(
            x = 242f,
            y = 289f,
            width = 148f,
            height = 238f,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            onClick = { onRecentlyViewedClick(recentItems[1.coerceAtMost(recentItems.lastIndex)]) },
        )
        DrawerClickZone(
            x = 437f,
            y = 289f,
            width = 148f,
            height = 238f,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            onClick = { onRecentlyViewedClick(recentItems[2.coerceAtMost(recentItems.lastIndex)]) },
        )

        staticContent.gameCenterEntries.take(4).forEachIndexed { index, entry ->
            val x = when (index) {
                0 -> 46f
                1 -> 242f
                2 -> 438f
                else -> 634f
            }
            DrawerClickZone(
                x = x,
                y = 837f,
                width = 148f,
                height = 170f,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                onClick = { onGameEntryClick(entry) },
            )
        }

        DrawerClickZone(
            x = 44f,
            y = 1214f,
            width = 752f,
            height = 106f,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            onClick = { onCommonFunctionClick(staticContent.commonFunctionEntries[0]) },
        )
        DrawerClickZone(
            x = 44f,
            y = 1334f,
            width = 752f,
            height = 106f,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            onClick = { onCommonFunctionClick(staticContent.commonFunctionEntries[1]) },
        )

        DrawerClickZone(
            x = 692f,
            y = 764f,
            width = 96f,
            height = 58f,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            onClick = {
                staticContent.gameCenterEntries.firstOrNull()?.let(onGameEntryClick)
            },
        )
    }
}

@Composable
private fun DrawerClickZone(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    maxWidth: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .offset(
                x = maxWidth * (x / DRAWER_REFERENCE_WIDTH),
                y = maxHeight * (y / DRAWER_REFERENCE_HEIGHT),
            )
            .size(
                width = maxWidth * (width / DRAWER_REFERENCE_WIDTH),
                height = maxHeight * (height / DRAWER_REFERENCE_HEIGHT),
            )
            .clickable(onClick = onClick),
    )
}
