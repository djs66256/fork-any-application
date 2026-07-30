package com.djs66256.short_drama.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.core.theme.SearchPageBackground
import com.djs66256.short_drama.core.theme.SearchPrimaryText
import com.djs66256.short_drama.feature.search.viewmodel.SearchHomeEvent
import com.djs66256.short_drama.feature.search.viewmodel.SearchHomeViewModel
import kotlinx.coroutines.flow.collect

@Composable
fun SearchHomeScreen(
    onBack: () -> Unit,
    onSubmitQuery: (String) -> Unit,
    onOpenQuickEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchHomeEvent.OpenQuickEntry -> onOpenQuickEntry(event.route)
                is SearchHomeEvent.OpenSearchResult -> onSubmitQuery(event.route)
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(SearchPageBackground),
        containerColor = SearchPageBackground,
        topBar = {
            SearchHomeTopBar(
                query = uiState.draftQuery,
                onBack = onBack,
                onQueryChange = viewModel::onQueryChange,
                onSubmit = viewModel::submitDraftQuery,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            item {
                SearchQuickEntrySection(
                    entries = uiState.quickEntries,
                    onOpenEntry = { entry -> viewModel.openQuickEntry(entry.type) },
                )
            }
            item {
                SearchHistorySection(
                    history = uiState.history,
                    onClickHistory = viewModel::submitHistory,
                    onClearHistory = viewModel::clearHistory,
                )
            }
            item {
                GuessLikeSection(
                    hotSearches = uiState.hotSearches,
                    onClickItem = viewModel::submitHotSearch,
                )
            }
            item {
                HotSearchSection(
                    hotSearches = uiState.hotSearches,
                    isLoading = uiState.isHotSearchLoading,
                    errorMessage = uiState.hotSearchErrorMessage,
                    onClickHotSearch = viewModel::submitHotSearch,
                    onRetry = viewModel::retryHotSearch,
                )
            }
        }
    }
}

@Composable
private fun SearchHomeTopBar(
    query: String,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SearchPageBackground)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "返回",
                tint = SearchPrimaryText,
            )
        }
        SearchInputBar(
            query = query,
            isSubmitting = false,
            placeholder = "应天阙：徒弟词条全是天花板",
            modifier = Modifier.weight(1f),
            onQueryChange = onQueryChange,
            onSubmit = onSubmit,
        )
    }
}
