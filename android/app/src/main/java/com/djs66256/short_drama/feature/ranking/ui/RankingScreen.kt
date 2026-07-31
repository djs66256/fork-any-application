package com.djs66256.short_drama.feature.ranking.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.HelpOutline
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.djs66256.short_drama.feature.ranking.model.RankingDetailTagTone
import com.djs66256.short_drama.feature.ranking.model.RankingDetailTagUiModel
import com.djs66256.short_drama.feature.ranking.model.RankingDramaItemUiModel
import com.djs66256.short_drama.feature.ranking.model.RankingMetricVisual
import com.djs66256.short_drama.feature.ranking.model.RankingPosterStyle
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

    RankingScreenContent(
        uiState = uiState,
        onBack = onBack,
        onContentTypeSelected = viewModel::onContentTypeSelected,
        onRankingTypeSelected = viewModel::onRankingTypeSelected,
        onRetry = viewModel::retry,
        onRetryAppend = viewModel::retryAppend,
        onLoadNextPage = viewModel::loadNextPageIfNeeded,
        onOpenPlay = onOpenPlay,
        onBook = viewModel::onBookClick,
        modifier = modifier,
    )
}

@Composable
fun RankingScreenContent(
    uiState: RankingUiState,
    onBack: () -> Unit,
    onContentTypeSelected: (RankingContentType) -> Unit,
    onRankingTypeSelected: (RankingType) -> Unit,
    onRetry: () -> Unit,
    onRetryAppend: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onBook: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onSelected = onContentTypeSelected,
                modifier = Modifier.padding(horizontal = 0.dp),
            )
            RankingTypeTabs(
                selected = uiState.selectedRankingType,
                onSelected = onRankingTypeSelected,
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
            )
            RankingContent(
                uiState = uiState,
                onRetry = onRetry,
                onRetryAppend = onRetryAppend,
                onLoadNextPage = onLoadNextPage,
                onOpenPlay = onOpenPlay,
                onBook = onBook,
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
    val headerPalette = rankingHeaderPalette(selectedRankingType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(188.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(headerPalette.backgroundStart, headerPalette.backgroundEnd),
                ),
            ),
    ) {
        RankingHeaderArtwork(
            rankingType = selectedRankingType,
            palette = headerPalette,
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 8.dp),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = RankingTextPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
        ) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "打开新页面",
                    tint = RankingTextPrimary,
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, end = 24.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = rankingBannerTitle(selectedRankingType),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                ),
                color = RankingTextPrimary,
            )
            Text(
                text = rankingBannerSubtitle(
                    contentType = selectedContentType,
                    rankingType = selectedRankingType,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = RankingTextSecondary.copy(alpha = 0.78f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-18).dp, y = (-22).dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.10f),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                tint = RankingTextSecondary,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun RankingHeaderArtwork(
    rankingType: RankingType,
    palette: RankingHeaderPalette,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (rankingType) {
            RankingType.HOT -> {
                val path = Path().apply {
                    moveTo(size.width * 0.52f, size.height * 0.24f)
                    cubicTo(
                        size.width * 0.70f,
                        size.height * 0.02f,
                        size.width * 0.90f,
                        size.height * 0.22f,
                        size.width * 0.94f,
                        size.height * 0.38f,
                    )
                    cubicTo(
                        size.width * 0.83f,
                        size.height * 0.32f,
                        size.width * 0.72f,
                        size.height * 0.40f,
                        size.width * 0.62f,
                        size.height * 0.58f,
                    )
                    cubicTo(
                        size.width * 0.56f,
                        size.height * 0.48f,
                        size.width * 0.49f,
                        size.height * 0.38f,
                        size.width * 0.52f,
                        size.height * 0.24f,
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.ribbonStart.copy(alpha = 0.92f),
                            palette.ribbonEnd.copy(alpha = 0.76f),
                        ),
                    ),
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(size.width * 0.58f, size.height * 0.48f),
                    size = Size(size.width * 0.34f, size.height * 0.08f),
                    cornerRadius = CornerRadius(80f, 80f),
                )
            }
            RankingType.RECOMMEND -> {
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = size.width * 0.19f,
                    center = Offset(size.width * 0.66f, size.height * 0.36f),
                    style = Stroke(width = 8f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.width * 0.13f,
                    center = Offset(size.width * 0.62f, size.height * 0.44f),
                    style = Stroke(width = 6f),
                )
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.ribbonStart.copy(alpha = 0.82f),
                            palette.ribbonEnd.copy(alpha = 0.84f),
                        ),
                    ),
                    startAngle = 286f,
                    sweepAngle = 112f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.45f, size.height * 0.12f),
                    size = Size(size.width * 0.44f, size.width * 0.44f),
                    style = Stroke(width = 24f, cap = StrokeCap.Round),
                )
            }
            RankingType.BOOKING -> {
                repeat(6) { index ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f + index * 0.02f),
                        start = Offset(size.width * 0.52f, size.height * (0.10f + index * 0.05f)),
                        end = Offset(size.width * 0.98f, size.height * (0.10f + index * 0.02f)),
                        strokeWidth = 10f,
                        cap = StrokeCap.Round,
                    )
                }
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFCE9B2).copy(alpha = 0.80f),
                            Color(0xFFFF7D8F).copy(alpha = 0.78f),
                        ),
                    ),
                    topLeft = Offset(size.width * 0.58f, size.height * 0.18f),
                    size = Size(size.width * 0.28f, size.height * 0.26f),
                    cornerRadius = CornerRadius(18f, 18f),
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.38f),
                    start = Offset(size.width * 0.61f, size.height * 0.24f),
                    end = Offset(size.width * 0.84f, size.height * 0.24f),
                    strokeWidth = 5f,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.38f),
                    start = Offset(size.width * 0.61f, size.height * 0.38f),
                    end = Offset(size.width * 0.84f, size.height * 0.38f),
                    strokeWidth = 5f,
                )
            }
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
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-30).dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        color = RankingCardBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            RankingContentType.entries.forEach { contentType ->
                val isSelected = contentType == selected
                Text(
                    text = rankingContentTypeLabel(contentType),
                    modifier = Modifier.clickable { onSelected(contentType) },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        fontSize = 18.sp,
                    ),
                    color = if (isSelected) RankingTextPrimary else RankingTextSecondary,
                )
            }
            Text(
                text = "演员",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp),
                color = RankingTextSecondary,
            )
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
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RankingType.entries.forEach { rankingType ->
            val isSelected = rankingType == selected
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelected(rankingType) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) RankingSelectedPill else RankingIdlePill,
            ) {
                Text(
                    text = rankingType.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    ),
                    color = if (isSelected) RankingAccentStrong else RankingTextSecondary,
                )
            }
        }
        RankingGhostPill(text = "新剧榜")
        RankingGhostPill(text = "热搜榜")
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = RankingCardBackground,
        ) {
            Text(
                text = "分类⌄",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = RankingTextPrimary,
            )
        }
    }
}

@Composable
private fun RankingGhostPill(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RankingIdlePill,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
            color = RankingTextSecondary,
        )
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
            .padding(horizontal = 16.dp)
            .offset(y = (-4).dp),
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
                    .background(RankingPageBackground.copy(alpha = 0.85f)),
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
        contentPadding = PaddingValues(bottom = 26.dp),
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
                modifier = Modifier.size(width = 116.dp, height = 154.dp),
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
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                            ),
                            color = RankingTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.secondaryText.isNotBlank()) {
                            Text(
                                text = item.secondaryText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = RankingTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    MetricBadge(
                        label = item.metricLabel,
                        value = item.metricValue,
                        visual = item.metricVisual,
                        rank = item.rank,
                    )
                }
                Text(
                    text = item.description.ifBlank { "暂无简介" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = RankingTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RankingDetailTags(tags = item.detailTags)
                if (showBookingButton) {
                    RankingBookingBar(
                        item = item,
                        bookingInFlight = bookingInFlight,
                        onBook = onBook,
                    )
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
    val posterColors = rankingPosterColors(item.posterStyle)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(posterColors.first, posterColors.second),
                ),
            ),
    ) {
        RankingPosterArtwork(
            rank = item.rank,
            posterStyle = item.posterStyle,
            modifier = Modifier.fillMaxSize(),
        )
        RankBadge(
            rank = item.rank,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(0.dp),
        )
        Text(
            text = item.posterTitle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                lineHeight = 16.sp,
            ),
            color = Color.White,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RankingPosterArtwork(
    rank: Int,
    posterStyle: RankingPosterStyle,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val accent = rankingPosterAccent(posterStyle)
        when (rank) {
            1 -> {
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = size.width * 0.54f,
                    center = Offset(size.width * 0.72f, size.height * 0.28f),
                )
                drawLine(
                    color = accent.copy(alpha = 0.46f),
                    start = Offset(size.width * 0.12f, size.height * 0.12f),
                    end = Offset(size.width * 0.90f, size.height * 0.58f),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round,
                )
            }
            2 -> {
                repeat(4) { index ->
                    drawRoundRect(
                        color = accent.copy(alpha = 0.14f + index * 0.08f),
                        topLeft = Offset(size.width * (0.08f + index * 0.10f), size.height * (0.14f + index * 0.08f)),
                        size = Size(size.width * 0.84f, size.height * 0.12f),
                        cornerRadius = CornerRadius(22f, 22f),
                    )
                }
            }
            3 -> {
                val path = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.30f)
                    quadraticTo(size.width * 0.52f, size.height * 0.10f, size.width * 0.86f, size.height * 0.34f)
                    quadraticTo(size.width * 0.54f, size.height * 0.42f, size.width * 0.20f, size.height * 0.88f)
                    close()
                }
                drawPath(path = path, color = accent.copy(alpha = 0.32f))
            }
            else -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            accent.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(size.width * 0.10f, size.height * 0.14f),
                    size = Size(size.width * 0.72f, size.height * 0.22f),
                    cornerRadius = CornerRadius(24f, 24f),
                )
            }
        }

        drawCircle(
            color = Color.Black.copy(alpha = 0.08f),
            radius = size.width * 0.58f,
            center = Offset(size.width * 0.76f, size.height * 0.84f),
        )
    }
}

@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val background = when (rank) {
        1 -> Color(0xFFFFB24A)
        2 -> Color(0xFF10D3B0)
        3 -> Color(0xFF4D98FF)
        else -> Color(0xFF505463)
    }
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 0.dp, bottomEnd = 12.dp, bottomStart = 0.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = background,
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White,
        )
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
    visual: RankingMetricVisual,
    rank: Int,
) {
    val numberSize = when (rank) {
        1 -> 20.sp
        2, 3 -> 19.sp
        else -> 18.sp
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = when (visual) {
                    RankingMetricVisual.FLAME -> Icons.Filled.LocalFireDepartment
                    RankingMetricVisual.CALENDAR -> Icons.Filled.CalendarMonth
                },
                contentDescription = null,
                tint = RankingAccentStrong,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = numberSize,
                ),
                color = RankingAccentStrong,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = RankingAccentStrong,
        )
    }
}

@Composable
private fun RankingDetailTags(tags: List<RankingDetailTagUiModel>) {
    if (tags.isEmpty()) {
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            val palette = rankingTagPalette(tag.tone)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = palette.background,
                border = BorderStroke(1.dp, palette.border),
            ) {
                Text(
                    text = tag.text,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.text,
                )
            }
        }
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
        shape = RoundedCornerShape(12.dp),
        color = RankingBookingStrip,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.bookingHintText.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = RankingBookingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onBook,
                enabled = item.id.isNotBlank() && !item.isBooked && !bookingInFlight,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.isBooked) RankingBookedButton else RankingSelectedPill,
                    contentColor = if (item.isBooked) RankingTextSecondary else RankingAccentStrong,
                    disabledContainerColor = if (item.isBooked) RankingBookedButton else RankingSelectedPill,
                    disabledContentColor = if (item.isBooked) RankingTextSecondary else RankingAccentStrong,
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            ) {
                Text(
                    text = when {
                        bookingInFlight -> "预约中"
                        item.isBooked -> "已预约"
                        else -> "预约"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                )
            }
        }
    }
}

@Composable
private fun RankingLoadingState(isRefreshing: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
        RankingPreviewCards(count = 5)
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = RankingCardBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = RankingTextPrimary,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RankingTextSecondary,
                )
                if (action != null) {
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
                    .size(width = 116.dp, height = 154.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = rankingPosterColors(RankingPosterStyle.fromRank(rank)).toList(),
                        ),
                    ),
            ) {
                RankBadge(rank = rank)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RankingSkeletonLine(width = 0.68f, height = 20.dp)
                RankingSkeletonLine(width = 0.44f, height = 16.dp)
                RankingSkeletonLine(width = 0.94f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RankingSkeletonChip(width = 72.dp)
                    RankingSkeletonChip(width = 96.dp)
                    RankingSkeletonChip(width = 108.dp)
                }
                RankingSkeletonLine(width = 0.92f, height = 42.dp)
            }
        }
    }
}

@Composable
private fun RankingSkeletonLine(
    width: Float,
    height: Dp = 14.dp,
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
private fun RankingSkeletonChip(width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
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
    RankingType.HOT -> "❴红果热播榜❵"
    RankingType.RECOMMEND -> "❴红果推荐榜❵"
    RankingType.BOOKING -> "❴红果预约榜❵"
}

private fun rankingBannerSubtitle(
    contentType: RankingContentType,
    rankingType: RankingType,
): String = when (rankingType) {
    RankingType.HOT -> "7月24日已更新·基于红果站内观看/互动等综合热度排序"
    RankingType.RECOMMEND -> "7月24日已更新·基于红果观看/互动以及个人兴趣排序"
    RankingType.BOOKING -> "基于红果预约/播放等综合期待值排序"
}

private fun rankingContentTypeLabel(contentType: RankingContentType): String = when (contentType) {
    RankingContentType.ALL -> "全部"
    RankingContentType.LIVE_ACTION -> "真人剧"
    RankingContentType.AI -> "AI剧"
}

private data class RankingHeaderPalette(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val ribbonStart: Color,
    val ribbonEnd: Color,
)

private fun rankingHeaderPalette(rankingType: RankingType): RankingHeaderPalette = when (rankingType) {
    RankingType.HOT -> RankingHeaderPalette(
        backgroundStart = Color(0xFFF9D9D2),
        backgroundEnd = Color(0xFFF6C7CD),
        ribbonStart = Color(0xFFFF847B),
        ribbonEnd = Color(0xFFDDE7FF),
    )
    RankingType.RECOMMEND -> RankingHeaderPalette(
        backgroundStart = Color(0xFFB9F2DF),
        backgroundEnd = Color(0xFFD2F7E6),
        ribbonStart = Color(0xFFFFE6A2),
        ribbonEnd = Color(0xFFFFC6B9),
    )
    RankingType.BOOKING -> RankingHeaderPalette(
        backgroundStart = Color(0xFF70E2F4),
        backgroundEnd = Color(0xFFA4ECFF),
        ribbonStart = Color(0xFFFCE7A8),
        ribbonEnd = Color(0xFFFF8AA0),
    )
}

private fun rankingPosterColors(style: RankingPosterStyle): Pair<Color, Color> = when (style) {
    RankingPosterStyle.SUNSET -> Color(0xFF343341) to Color(0xFFC27432)
    RankingPosterStyle.EMERALD -> Color(0xFF14262A) to Color(0xFF0FA57D)
    RankingPosterStyle.RIVIERA -> Color(0xFF1E2543) to Color(0xFF3D88F8)
    RankingPosterStyle.VIOLET -> Color(0xFF271E44) to Color(0xFF7F58E7)
    RankingPosterStyle.BLUSH -> Color(0xFF472941) to Color(0xFFE5759A)
    RankingPosterStyle.MIDNIGHT -> Color(0xFF1E1F27) to Color(0xFF595F79)
    RankingPosterStyle.SCARLET -> Color(0xFF491D1F) to Color(0xFFD74C4F)
    RankingPosterStyle.FOREST -> Color(0xFF183227) to Color(0xFF47906C)
    RankingPosterStyle.SKY -> Color(0xFF23405A) to Color(0xFF6AA7FF)
    RankingPosterStyle.PLUM -> Color(0xFF3C284E) to Color(0xFFAE7BDE)
    RankingPosterStyle.AMBER -> Color(0xFF4B361F) to Color(0xFFE9A54F)
    RankingPosterStyle.PEARL -> Color(0xFF4B4E5A) to Color(0xFFB7C1D8)
}

private fun rankingPosterAccent(style: RankingPosterStyle): Color = when (style) {
    RankingPosterStyle.SUNSET -> Color(0xFFFFD98C)
    RankingPosterStyle.EMERALD -> Color(0xFF77F0CE)
    RankingPosterStyle.RIVIERA -> Color(0xFF9AD7FF)
    RankingPosterStyle.VIOLET -> Color(0xFFDAB2FF)
    RankingPosterStyle.BLUSH -> Color(0xFFFFBED5)
    RankingPosterStyle.MIDNIGHT -> Color(0xFFD8DBE5)
    RankingPosterStyle.SCARLET -> Color(0xFFFFA8A2)
    RankingPosterStyle.FOREST -> Color(0xFFA8F5C8)
    RankingPosterStyle.SKY -> Color(0xFFC5E2FF)
    RankingPosterStyle.PLUM -> Color(0xFFF0C7FF)
    RankingPosterStyle.AMBER -> Color(0xFFFFE4A8)
    RankingPosterStyle.PEARL -> Color(0xFFF1F5FF)
}

private data class RankingTagPalette(
    val background: Color,
    val border: Color,
    val text: Color,
)

private fun rankingTagPalette(tone: RankingDetailTagTone): RankingTagPalette = when (tone) {
    RankingDetailTagTone.WARM -> RankingTagPalette(
        background = Color(0xFFFFF5E8),
        border = Color(0xFFF6DEC4),
        text = Color(0xFFD49543),
    )
    RankingDetailTagTone.MINT -> RankingTagPalette(
        background = Color(0xFFE8FFF6),
        border = Color(0xFFB7EFD6),
        text = Color(0xFF12B57A),
    )
    RankingDetailTagTone.CORAL -> RankingTagPalette(
        background = Color(0xFFFFF1EC),
        border = Color(0xFFF5D0BF),
        text = Color(0xFFFF7E2F),
    )
}

private val RankingPageBackground = Color(0xFFF6F6F6)
private val RankingCardBackground = Color(0xFFFFFFFF)
private val RankingTextPrimary = Color(0xFF171717)
private val RankingTextSecondary = Color(0xFF989898)
private val RankingTextMuted = Color(0xFFA6A6A6)
private val RankingAccentStrong = Color(0xFFFF7F1F)
private val RankingSelectedPill = Color(0xFFFCEBDD)
private val RankingIdlePill = Color(0xFFF3F3F3)
private val RankingBookingStrip = Color(0xFFF6F6F6)
private val RankingBookingText = Color(0xFF969696)
private val RankingBookedButton = Color(0xFFE7E7E7)
private val RankingSkeletonBlock = Color(0xFFF1F2F4)
