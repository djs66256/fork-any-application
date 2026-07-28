package com.djs66256.short_drama.feature.theater.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.domain.model.TheaterChannel
import com.djs66256.short_drama.feature.theater.model.TheaterDramaItemUiModel
import com.djs66256.short_drama.feature.theater.viewmodel.TheaterUiState
import com.djs66256.short_drama.navigation.TheaterShortcutRoute

@Composable
fun TheaterContent(
    uiState: TheaterUiState,
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
    onChannelSelected: (TheaterChannel) -> Unit,
    onShortcutClick: (TheaterShortcutRoute) -> Unit,
    onRetry: () -> Unit,
    onLoadNextPage: () -> Unit,
    onDramaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fullSpanItem {
            TheaterTopBar(
                onSearchClick = onSearchClick,
                onScanClick = onScanClick,
            )
        }
        fullSpanItem {
            TheaterChannelTabs(
                selectedChannel = uiState.selectedChannel,
                onChannelSelected = onChannelSelected,
            )
        }
        fullSpanItem {
            TheaterShortcutGrid(
                onShortcutClick = onShortcutClick,
            )
        }

        when {
            uiState.isLoading && !uiState.hasLoadedOnce -> {
                fullSpanItem {
                    TheaterLoadingState()
                }
            }
            uiState.errorMessage != null && uiState.items.isEmpty() -> {
                fullSpanItem {
                    TheaterErrorState(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = onRetry,
                    )
                }
            }
            uiState.hasLoadedOnce && uiState.items.isEmpty() -> {
                fullSpanItem {
                    TheaterEmptyState(channel = uiState.selectedChannel)
                }
            }
            else -> {
                items(
                    count = uiState.items.size,
                    key = { index -> uiState.items[index].id.ifBlank { index.toString() } },
                ) { index ->
                    val item = uiState.items[index]
                    TheaterDramaCard(
                        item = item,
                        onOpenPlay = { onDramaClick(item.id) },
                    )

                    if (index == uiState.items.lastIndex && uiState.hasNextPage) {
                        LaunchedEffect(uiState.selectedChannel, uiState.page, item.id) {
                            onLoadNextPage()
                        }
                    }
                }
                fullSpanItem {
                    TheaterAppendFooter(
                        isAppending = uiState.isAppending,
                        appendErrorMessage = uiState.appendErrorMessage,
                        hasNextPage = uiState.hasNextPage,
                        hasItems = uiState.items.isNotEmpty(),
                        onRetry = onLoadNextPage,
                    )
                }
            }
        }
    }
}

@Composable
fun TheaterTopBar(
    onSearchClick: () -> Unit,
    onScanClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClick = onSearchClick),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Text(
                    text = "搜索短剧、题材或关键词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onScanClick) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = "识图入口",
            )
        }
    }
}

@Composable
fun TheaterChannelTabs(
    selectedChannel: TheaterChannel,
    onChannelSelected: (TheaterChannel) -> Unit,
) {
    ScrollableTabRow(selectedTabIndex = TheaterChannel.entries.indexOf(selectedChannel)) {
        TheaterChannel.entries.forEach { channel ->
            Tab(
                selected = channel == selectedChannel,
                onClick = { onChannelSelected(channel) },
                text = { Text(channel.label) },
            )
        }
    }
}

@Composable
fun TheaterShortcutGrid(
    onShortcutClick: (TheaterShortcutRoute) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "快捷入口",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        TheaterShortcutRoute.quickEntries.chunked(2).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowEntries.forEach { shortcut ->
                    FilledTonalButton(
                        onClick = { onShortcutClick(shortcut) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(shortcut.title)
                    }
                }
            }
        }
    }
}

@Composable
fun TheaterDramaCard(
    item: TheaterDramaItemUiModel,
    onOpenPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.id.isNotBlank(), onClick = onOpenPlay),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = if (item.coverUrl.isBlank()) "暂无封面" else "封面已配置",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                HeatChip(
                    text = item.heatText,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.metaText.isNotBlank()) {
                Text(
                    text = item.metaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HeatChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "热度",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
fun TheaterLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = "正在加载剧场内容...",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun TheaterEmptyState(channel: TheaterChannel) {
    val title = if (channel == TheaterChannel.ALL) "当前暂无内容" else "${channel.label}频道内容筹备中"
    val description = if (channel == TheaterChannel.ALL) {
        "稍后再来看看新的短剧内容。"
    } else {
        "当前频道首版暂未上线内容，可以先看看其它频道。"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun TheaterErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text("重试")
        }
    }
}

@Composable
private fun TheaterAppendFooter(
    isAppending: Boolean,
    appendErrorMessage: String?,
    hasNextPage: Boolean,
    hasItems: Boolean,
    onRetry: () -> Unit,
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
                Text(
                    text = "正在加载更多...",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                OutlinedButton(onClick = onRetry) {
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

private fun LazyGridScope.fullSpanItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        content()
    }
}
