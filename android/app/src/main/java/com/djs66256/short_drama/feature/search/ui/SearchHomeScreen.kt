package com.djs66256.short_drama.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.feature.search.viewmodel.SearchHomeEvent
import com.djs66256.short_drama.feature.search.viewmodel.SearchHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("搜索发现") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                SearchInputBar(
                    query = uiState.draftQuery,
                    isSubmitting = false,
                    placeholder = "搜索短剧、题材或关键词",
                    onQueryChange = viewModel::onQueryChange,
                    onSubmit = viewModel::submitDraftQuery,
                )
            }
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
