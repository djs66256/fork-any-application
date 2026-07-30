package com.djs66256.short_drama.feature.booking.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.BookingAssetStatus
import com.djs66256.short_drama.feature.booking.model.BookingAssetItemUiModel
import com.djs66256.short_drama.feature.booking.model.BookingAssetsEffect
import com.djs66256.short_drama.feature.booking.model.BookingAssetsUiState
import com.djs66256.short_drama.feature.booking.ui.components.BookingAssetCard
import com.djs66256.short_drama.feature.booking.ui.components.BookingAssetsEmptyState
import com.djs66256.short_drama.feature.booking.ui.components.BookingAssetsErrorState
import com.djs66256.short_drama.feature.booking.ui.components.BookingAssetsLoginGate
import com.djs66256.short_drama.feature.booking.ui.components.BookingStatusTabs
import com.djs66256.short_drama.feature.booking.viewmodel.BookingAssetsViewModel
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingAssetsScreen(
    onBack: () -> Unit,
    onRequireLogin: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BookingAssetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BookingAssetsEffect.RequireLogin -> onRequireLogin(effect.returnRoute)
                is BookingAssetsEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("我的预约") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            BookingStatusTabs(
                selectedStatus = uiState.selectedStatus,
                summary = uiState.summary,
                onStatusSelected = viewModel::onStatusSelected,
            )
            BookingAssetsContent(
                uiState = uiState,
                onRetry = viewModel::retry,
                onRetryAppend = viewModel::retryAppend,
                onLoadNextPage = viewModel::loadNextPageIfNeeded,
                onLoginClick = viewModel::onLoginClick,
            )
        }
    }
}

@Composable
private fun BookingAssetsContent(
    uiState: BookingAssetsUiState,
    onRetry: () -> Unit,
    onRetryAppend: () -> Unit,
    onLoadNextPage: () -> Unit,
    onLoginClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        when {
            uiState.showLoginGate -> BookingAssetsLoginGate(onLoginClick = onLoginClick)
            uiState.showInitialLoading -> BookingLoadingState(isRefreshing = false)
            uiState.showError -> BookingAssetsErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
            )
            uiState.showEmpty -> BookingAssetsEmptyState(status = uiState.selectedStatus)
            else -> BookingAssetsList(
                items = uiState.items,
                selectedStatus = uiState.selectedStatus,
                isAppending = uiState.isAppending,
                appendErrorMessage = uiState.appendErrorMessage,
                hasNextPage = uiState.hasNextPage,
                onRetryAppend = onRetryAppend,
                onLoadNextPage = onLoadNextPage,
            )
        }

        if (uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                BookingLoadingState(isRefreshing = true)
            }
        }
    }
}

@Composable
private fun BookingAssetsList(
    items: List<BookingAssetItemUiModel>,
    selectedStatus: BookingAssetStatus,
    isAppending: Boolean,
    appendErrorMessage: String?,
    hasNextPage: Boolean,
    onRetryAppend: () -> Unit,
    onLoadNextPage: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(items = items, key = { _, item -> item.dramaId }) { index, item ->
            BookingAssetCard(item = item)

            val shouldLoadNextPage = index == items.lastIndex && hasNextPage
            if (shouldLoadNextPage) {
                LaunchedEffect(selectedStatus, index, item.dramaId, items.size) {
                    onLoadNextPage()
                }
            }
        }

        item {
            BookingAppendFooter(
                isAppending = isAppending,
                appendErrorMessage = appendErrorMessage,
                hasNextPage = hasNextPage,
                hasItems = items.isNotEmpty(),
                onRetryAppend = onRetryAppend,
            )
        }
    }
}

@Composable
private fun BookingLoadingState(isRefreshing: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRefreshing) "正在刷新预约列表..." else "正在加载预约列表...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BookingAppendFooter(
    isAppending: Boolean,
    appendErrorMessage: String?,
    hasNextPage: Boolean,
    hasItems: Boolean,
    onRetryAppend: () -> Unit,
) {
    when {
        isAppending -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("正在加载更多...")
            }
        }
        appendErrorMessage != null && hasItems -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = appendErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(onClick = onRetryAppend) {
                    Text("重试加载更多")
                }
            }
        }
        !hasNextPage && hasItems -> {
            Text(
                text = "没有更多了",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
