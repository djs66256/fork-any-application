package com.djs66256.short_drama.feature.search.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.feature.home.ui.HomeDramaCard
import com.djs66256.short_drama.feature.search.viewmodel.SearchResultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    onBack: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("搜索结果") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                SearchInputBar(
                    query = uiState.draftQuery,
                    isSubmitting = uiState.isLoading,
                    placeholder = "搜索短剧、题材或关键词",
                    onQueryChange = viewModel::onDraftQueryChange,
                    onSubmit = viewModel::submitDraftQuery,
                )
            }

            when {
                uiState.isLoading -> {
                    item {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
                    }
                }
                uiState.errorMessage != null -> {
                    item {
                        SearchResultErrorState(
                            message = uiState.errorMessage.orEmpty(),
                            onRetry = viewModel::retry,
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                }
                uiState.hasLoadedOnce && uiState.items.isEmpty() -> {
                    item {
                        SearchResultEmptyState(
                            query = uiState.query,
                            modifier = Modifier.padding(top = 32.dp),
                        )
                    }
                }
                else -> {
                    items(items = uiState.items, key = { it.id }) { drama ->
                        HomeDramaCard(
                            drama = drama,
                            onPlay = { onOpenPlay(drama.id) },
                            onDetail = { onOpenDetail(drama.id) },
                            onComment = {},
                        )
                    }
                }
            }
        }
    }
}
