package com.djs66256.short_drama.feature.menu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.djs66256.short_drama.feature.menu.ui.components.MenuCommonFunctionsSection
import com.djs66256.short_drama.feature.menu.ui.components.MenuGameCenterSection
import com.djs66256.short_drama.feature.menu.ui.components.MenuLoginHeader
import com.djs66256.short_drama.feature.menu.ui.components.MenuMessagePreview
import com.djs66256.short_drama.feature.menu.ui.components.MenuRecentlyViewedSection
import com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelEvent
import com.djs66256.short_drama.feature.menu.viewmodel.MenuPanelViewModel
import com.djs66256.short_drama.navigation.PendingRoute
import kotlinx.coroutines.flow.collect

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "菜单",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        MenuLoginHeader(
            entry = staticContent.loginHeader,
            onClick = onLoginClick,
        )
        MenuMessagePreview(
            entry = staticContent.messagePreview,
            onClick = onMessageClick,
        )
        MenuRecentlyViewedSection(
            uiState = uiState,
            onRetry = onRetryRecentlyViewed,
            onItemClick = onRecentlyViewedClick,
        )
        MenuGameCenterSection(
            entries = staticContent.gameCenterEntries,
            onEntryClick = onGameEntryClick,
        )
        MenuCommonFunctionsSection(
            entries = staticContent.commonFunctionEntries,
            onEntryClick = onCommonFunctionClick,
        )
    }
}
