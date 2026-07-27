package com.djs66256.short_drama.feature.ranking.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingType
import com.djs66256.short_drama.feature.ranking.model.RankingDramaItemUiModel
import com.djs66256.short_drama.feature.ranking.viewmodel.RankingEffect
import com.djs66256.short_drama.feature.ranking.viewmodel.RankingUiState
import com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    onBack: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onRequireLogin: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RankingEffect.RequireLogin -> onRequireLogin(effect.returnRoute)
                is RankingEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("排行") },
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
            RankingContentTypeTabs(
                selected = uiState.selectedContentType,
                onSelected = viewModel::onContentTypeSelected,
            )
            RankingTypeTabs(
                selected = uiState.selectedRankingType,
                onSelected = viewModel::onRankingTypeSelected,
            )
            RankingContent(
                uiState = uiState,
                onRetry = viewModel::retry,
                onRetryAppend = viewModel::retryAppend,
                onLoadNextPage = viewModel::loadNextPageIfNeeded,
                onOpenPlay = onOpenPlay,
                onBook = viewModel::onBookClick,
            )
        }
    }
}

@Composable
private fun RankingContentTypeTabs(
    selected: RankingContentType,
    onSelected: (RankingContentType) -> Unit,
) {
    TabRow(selectedTabIndex = RankingContentType.entries.indexOf(selected)) {
        RankingContentType.entries.forEach { contentType ->
            Tab(
                selected = contentType == selected,
                onClick = { onSelected(contentType) },
                text = { Text(contentType.label) },
            )
        }
    }
}

@Composable
private fun RankingTypeTabs(
    selected: RankingType,
    onSelected: (RankingType) -> Unit,
) {
    TabRow(selectedTabIndex = RankingType.entries.indexOf(selected)) {
        RankingType.entries.forEach { rankingType ->
            Tab(
                selected = rankingType == selected,
                onClick = { onSelected(rankingType) },
                text = { Text(rankingType.label) },
            )
        }
    }
}

@Composable
private fun RankingContent(
    uiState: RankingUiState,
    onRetry: () -> Unit,
    onRetryAppend: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onBook: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        when {
            uiState.isLoading && !uiState.hasLoadedOnce -> RankingLoadingState(isRefreshing = false)
            uiState.errorMessage != null && uiState.items.isEmpty() -> RankingErrorState(
                message = uiState.errorMessage.orEmpty(),
                onRetry = onRetry,
            )
            uiState.hasLoadedOnce && uiState.items.isEmpty() -> RankingEmptyState()
            else -> RankingList(
                uiState = uiState,
                onRetryAppend = onRetryAppend,
                onLoadNextPage = onLoadNextPage,
                onOpenPlay = onOpenPlay,
                onBook = onBook,
            )
        }

        if (uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center,
            ) {
                RankingLoadingState(isRefreshing = true)
            }
        }
    }
}

@Composable
private fun RankingList(
    uiState: RankingUiState,
    onRetryAppend: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onBook: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            items = uiState.items,
            key = { _, item -> item.id.ifBlank { item.rank.toString() } },
        ) { index, item ->
            RankingDramaCard(
                item = item,
                showBookingButton = uiState.selectedRankingType == RankingType.BOOKING,
                bookingInFlight = item.id in uiState.bookingInFlightIds,
                onOpenPlay = {
                    if (item.id.isNotBlank()) {
                        onOpenPlay(item.id)
                    }
                },
                onBook = { onBook(item.id) },
            )

            val shouldLoadNextPage = index == uiState.items.lastIndex && uiState.hasNextPage
            if (shouldLoadNextPage) {
                LaunchedEffect(
                    uiState.selectedContentType,
                    uiState.selectedRankingType,
                    uiState.page,
                    item.id,
                ) {
                    onLoadNextPage()
                }
            }
        }

        item {
            RankingAppendFooter(
                isAppending = uiState.isAppending,
                appendErrorMessage = uiState.appendErrorMessage,
                hasNextPage = uiState.hasNextPage,
                hasItems = uiState.items.isNotEmpty(),
                onRetryAppend = onRetryAppend,
            )
        }
    }
}

@Composable
fun RankingDramaCard(
    item: RankingDramaItemUiModel,
    showBookingButton: Boolean,
    bookingInFlight: Boolean,
    onOpenPlay: () -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.id.isNotBlank(), onClick = onOpenPlay),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RankBadge(rank = item.rank)
            RankingCoverPlaceholder(item = item)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.description.ifBlank { "暂无简介" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.metaText.isNotBlank()) {
                    Text(
                        text = item.metaText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                MetricChip(label = item.metricLabel, value = item.metricValue)
                if (showBookingButton) {
                    OutlinedButton(
                        onClick = onBook,
                        enabled = item.id.isNotBlank() && !item.isBooked && !bookingInFlight,
                    ) {
                        when {
                            bookingInFlight -> Text("预约中...")
                            item.isBooked -> Text("已预约")
                            else -> Text("预约")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rank.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RankingCoverPlaceholder(item: RankingDramaItemUiModel) {
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 128.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = if (item.coverUrl.isBlank()) "暂无封面" else "封面已配置",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun RankingLoadingState(isRefreshing: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRefreshing) "正在刷新榜单..." else "正在加载榜单...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RankingEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "当前榜单暂无内容",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "试试切换内容类型或榜单维度看看。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RankingErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun RankingAppendFooter(
    isAppending: Boolean,
    appendErrorMessage: String?,
    hasNextPage: Boolean,
    hasItems: Boolean,
    onRetryAppend: () -> Unit,
) {
    when {
        isAppending -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.size(8.dp))
                Text("正在加载更多...")
            }
        }
        appendErrorMessage != null && hasItems -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
