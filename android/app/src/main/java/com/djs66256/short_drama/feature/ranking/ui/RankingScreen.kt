package com.djs66256.short_drama.feature.ranking.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingType
import com.djs66256.short_drama.feature.ranking.model.RankingDramaItemUiModel
import com.djs66256.short_drama.feature.ranking.viewmodel.RankingEffect
import com.djs66256.short_drama.feature.ranking.viewmodel.RankingUiState
import com.djs66256.short_drama.feature.ranking.viewmodel.RankingViewModel
import kotlinx.coroutines.flow.collect

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
        containerColor = RankingPageBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RankingPageBackground)
                .padding(innerPadding),
        ) {
            RankingHeader(
                selectedContentType = uiState.selectedContentType,
                selectedRankingType = uiState.selectedRankingType,
                onBack = onBack,
            )
            RankingContentTypeTabs(
                selected = uiState.selectedContentType,
                onSelected = viewModel::onContentTypeSelected,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            RankingTypeTabs(
                selected = uiState.selectedRankingType,
                onSelected = viewModel::onRankingTypeSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            RankingContent(
                uiState = uiState,
                onRetry = viewModel::retry,
                onRetryAppend = viewModel::retryAppend,
                onLoadNextPage = viewModel::loadNextPageIfNeeded,
                onOpenPlay = onOpenPlay,
                onBook = viewModel::onBookClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RankingHeader(
    selectedContentType: RankingContentType,
    selectedRankingType: RankingType,
    onBack: () -> Unit,
) {
    val bannerBrush = rankingBannerBrush(selectedRankingType)
    val title = rankingBannerTitle(selectedRankingType)
    val subtitle = rankingBannerSubtitle(selectedContentType, selectedRankingType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(176.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(bannerBrush),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.14f),
                        ),
                    ),
                ),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.88f),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = RankingTextPrimary,
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
        ) {
            Text(
                text = "榜",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                ),
                color = RankingTextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = RankingTextPrimary.copy(alpha = 0.62f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RankingContentTypeTabs(
    selected: RankingContentType,
    onSelected: (RankingContentType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = RankingCardBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RankingContentType.entries.forEach { contentType ->
                val isSelected = contentType == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) RankingAccent else RankingTabIdle)
                        .clickable { onSelected(contentType) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contentType.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) Color.White else RankingTextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankingTypeTabs(
    selected: RankingType,
    onSelected: (RankingType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RankingType.entries.forEach { rankingType ->
            val isSelected = rankingType == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelected(rankingType) },
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) RankingAccentSoft else RankingTabIdle,
            ) {
                Text(
                    text = rankingType.label,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (isSelected) RankingAccent else RankingTextSecondary,
                )
            }
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
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
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
                    .background(RankingPageBackground.copy(alpha = 0.80f)),
                contentAlignment = Alignment.TopCenter,
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
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            RankingListHint(
                selectedContentType = uiState.selectedContentType,
                selectedRankingType = uiState.selectedRankingType,
            )
        }

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
private fun RankingListHint(
    selectedContentType: RankingContentType,
    selectedRankingType: RankingType,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = RankingHintBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(RankingAccent),
            )
            Text(
                text = "${selectedContentType.label} · ${selectedRankingType.label} 按近期热度、反馈与完播表现综合排序",
                style = MaterialTheme.typography.bodySmall,
                color = RankingTextSecondary,
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = item.id.isNotBlank(), onClick = onOpenPlay),
        shape = RoundedCornerShape(22.dp),
        color = RankingCardBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RankingPoster(
                item = item,
                modifier = Modifier.size(width = 92.dp, height = 126.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = RankingTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    MetricBadge(
                        label = item.metricLabel,
                        value = item.metricValue,
                    )
                }
                if (item.metaText.isNotBlank()) {
                    Text(
                        text = item.metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = RankingTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = item.description.ifBlank { "暂无简介" },
                    style = MaterialTheme.typography.bodySmall,
                    color = RankingTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showBookingButton) {
                    RankingBookingBar(
                        item = item,
                        bookingInFlight = bookingInFlight,
                        onBook = onBook,
                    )
                } else {
                    MetricChip(label = item.metricLabel, value = item.metricValue)
                }
            }
        }
    }
}

@Composable
private fun RankingPoster(
    item: RankingDramaItemUiModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(rankPosterStart(item.rank), rankPosterEnd(item.rank)),
                ),
            ),
    ) {
        RankBadge(
            rank = item.rank,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.20f),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}

@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val background = when (rank) {
        1 -> Color(0xFFFFA53F)
        2 -> Color(0xFF14C8A8)
        3 -> Color(0xFF4A92FF)
        else -> Color(0xFF3A3D45)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = background,
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = Color.White,
        )
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            ),
            color = RankingAccentStrong,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = RankingAccentStrong,
        )
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RankingChipBackground)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = RankingChipText,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = RankingChipText,
        )
    }
}

@Composable
private fun RankingBookingBar(
    item: RankingDramaItemUiModel,
    bookingInFlight: Boolean,
    onBook: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = RankingChipBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "预约 · ${item.bookingCount}",
                style = MaterialTheme.typography.bodySmall,
                color = RankingChipText,
            )
            Button(
                onClick = onBook,
                enabled = item.id.isNotBlank() && !item.isBooked && !bookingInFlight,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.isBooked) RankingBookedButton else RankingAccentStrong,
                    contentColor = Color.White,
                    disabledContainerColor = if (item.isBooked) RankingBookedButton else RankingAccentMuted,
                    disabledContentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            ) {
                Text(
                    text = when {
                        bookingInFlight -> "预约中"
                        item.isBooked -> "已预约"
                        else -> "预约"
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

@Composable
private fun RankingLoadingState(isRefreshing: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isRefreshing) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = RankingCardBackground,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = RankingAccentStrong,
                    )
                    Text(
                        text = "正在刷新榜单...",
                        style = MaterialTheme.typography.bodySmall,
                        color = RankingTextSecondary,
                    )
                }
            }
        }
        RankingPreviewCards(count = 4)
    }
}

@Composable
private fun RankingEmptyState() {
    RankingUnavailableState(
        title = "当前榜单暂无内容",
        message = "可以切换内容类型或榜单维度，看看其它热门内容。",
        action = null,
    )
}

@Composable
private fun RankingErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    RankingUnavailableState(
        title = "榜单暂时无法刷新",
        message = message,
        action = {
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RankingAccentStrong,
                    contentColor = Color.White,
                ),
            ) {
                Text("重试")
            }
        },
    )
}

@Composable
private fun RankingUnavailableState(
    title: String,
    message: String,
    action: (@Composable () -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = RankingCardBackground,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = RankingTextPrimary,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = RankingTextSecondary,
                    )
                }
                if (action != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    action()
                }
            }
        }
        RankingPreviewCards(count = 4)
    }
}

@Composable
private fun RankingPreviewCards(count: Int) {
    repeat(count) { index ->
        RankingPreviewCard(rank = index + 1)
    }
}

@Composable
private fun RankingPreviewCard(rank: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = RankingCardBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 126.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(rankPosterStart(rank), rankPosterEnd(rank)),
                        ),
                    ),
            ) {
                RankBadge(
                    rank = rank,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RankingSkeletonLine(width = 0.64f, height = 18.dp)
                RankingSkeletonLine(width = 0.38f)
                RankingSkeletonLine(width = 0.88f)
                RankingSkeletonLine(width = 0.74f)
                Spacer(modifier = Modifier.height(4.dp))
                RankingSkeletonLine(width = 0.48f, height = 32.dp)
            }
        }
    }
}

@Composable
private fun RankingSkeletonLine(
    width: Float,
    height: Dp = 12.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(width)
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(RankingSkeletonBlock),
    )
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
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RankingCardBackground,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = RankingAccentStrong,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "正在加载更多榜单内容...",
                        style = MaterialTheme.typography.bodySmall,
                        color = RankingTextSecondary,
                    )
                }
            }
        }
        appendErrorMessage != null && hasItems -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = RankingCardBackground,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = appendErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = RankingTextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onRetryAppend,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RankingAccentStrong,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("重试加载更多")
                    }
                }
            }
        }
        !hasNextPage && hasItems -> {
            Text(
                text = "已经到底了",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = RankingTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun rankingBannerTitle(rankingType: RankingType): String = when (rankingType) {
    RankingType.HOT -> "热播榜"
    RankingType.RECOMMEND -> "推荐榜"
    RankingType.BOOKING -> "预约榜"
}

private fun rankingBannerSubtitle(
    contentType: RankingContentType,
    rankingType: RankingType,
): String = when (rankingType) {
    RankingType.HOT -> "${contentType.label}短剧近期观看与互动热度综合排序"
    RankingType.RECOMMEND -> "${contentType.label}短剧口碑与推荐反馈综合排序"
    RankingType.BOOKING -> "${contentType.label}短剧预约与播前期待值综合排序"
}

private fun rankingBannerBrush(rankingType: RankingType): Brush = when (rankingType) {
    RankingType.HOT -> Brush.linearGradient(
        colors = listOf(Color(0xFFFCE0D6), Color(0xFFF9C9CA), Color(0xFFEBCBFF)),
    )
    RankingType.RECOMMEND -> Brush.linearGradient(
        colors = listOf(Color(0xFFE4F4FF), Color(0xFFD6F6ED), Color(0xFFCEE1FF)),
    )
    RankingType.BOOKING -> Brush.linearGradient(
        colors = listOf(Color(0xFFD5F7FF), Color(0xFFB9E9FF), Color(0xFFFFD4DB)),
    )
}

private fun rankPosterStart(rank: Int): Color = when (rank % 4) {
    1 -> Color(0xFFFFB24A)
    2 -> Color(0xFF10CDB6)
    3 -> Color(0xFF5796FF)
    else -> Color(0xFF8E79FF)
}

private fun rankPosterEnd(rank: Int): Color = when (rank % 4) {
    1 -> Color(0xFFFF7A7D)
    2 -> Color(0xFF15A86C)
    3 -> Color(0xFF7B62FF)
    else -> Color(0xFFFF6D91)
}

private val RankingPageBackground = Color(0xFFF4F5F7)
private val RankingCardBackground = Color(0xFFFFFFFF)
private val RankingHintBackground = Color(0xFFFFF2F2)
private val RankingTextPrimary = Color(0xFF181B22)
private val RankingTextSecondary = Color(0xFF8B90A0)
private val RankingTextMuted = Color(0xFFA2A7B5)
private val RankingAccent = Color(0xFFFF8B2B)
private val RankingAccentStrong = Color(0xFFFF7A1B)
private val RankingAccentMuted = Color(0xFFF9BE92)
private val RankingAccentSoft = Color(0xFFFFF0E4)
private val RankingTabIdle = Color(0xFFF0F1F4)
private val RankingChipBackground = Color(0xFFFFF5EC)
private val RankingChipText = Color(0xFFC88733)
private val RankingBookedButton = Color(0xFFC8CDD8)
private val RankingSkeletonBlock = Color(0xFFF1F2F5)
