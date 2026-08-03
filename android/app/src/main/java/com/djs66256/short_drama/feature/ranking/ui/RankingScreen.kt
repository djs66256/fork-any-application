package com.djs66256.short_drama.feature.ranking.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RankingPageBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        val safeStatusBarPadding = with(density) {
            WindowInsets.statusBars.getTop(this).toDp()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(RankingPageBackground),
        ) {
            val headerTopPadding = rankingHeaderTopPadding(safeStatusBarPadding)
            RankingHeader(
                selectedContentType = uiState.selectedContentType,
                selectedRankingType = uiState.selectedRankingType,
                onBack = onBack,
                topInset = headerTopPadding,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-18).dp),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                    color = RankingCardBackground,
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RankingContentTypeTabs(
                            selected = uiState.selectedContentType,
                            onSelected = onContentTypeSelected,
                        )
                        RankingTypeTabs(
                            selected = uiState.selectedRankingType,
                            onSelected = onRankingTypeSelected,
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
        }
    }
}

@Composable
private fun RankingHeader(
    selectedContentType: RankingContentType,
    selectedRankingType: RankingType,
    onBack: () -> Unit,
    topInset: Dp,
) {
    val headerPalette = rankingHeaderPalette(selectedRankingType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(192.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(headerPalette.backgroundStart, headerPalette.backgroundEnd),
                    start = Offset.Zero,
                    end = Offset(1080f, 540f),
                ),
            ),
    ) {
        RankingHeaderArtwork(
            rankingType = selectedRankingType,
            palette = headerPalette,
            modifier = Modifier.fillMaxSize(),
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = topInset + 8.dp)
                .size(36.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = RankingTextPrimary,
                modifier = Modifier.size(30.dp),
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topInset + 4.dp, end = 18.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.16f),
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
                .padding(start = 28.dp, end = 118.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RankingBannerTitle(title = rankingBannerTitle(selectedRankingType))
            Text(
                text = rankingBannerSubtitle(
                    contentType = selectedContentType,
                    rankingType = selectedRankingType,
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                ),
                color = RankingTextSecondary.copy(alpha = 0.84f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 30.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.10f),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                tint = RankingTextSecondary.copy(alpha = 0.95f),
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun RankingBannerTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RankingBannerLaurel()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                lineHeight = 30.sp,
            ),
            color = RankingTextPrimary,
        )
        RankingBannerLaurel(mirrored = true)
    }
}

@Composable
private fun RankingBannerLaurel(mirrored: Boolean = false) {
    Canvas(
        modifier = Modifier
            .size(width = 16.dp, height = 24.dp)
            .graphicsLayer(scaleX = if (mirrored) -1f else 1f),
    ) {
        val leafColor = RankingTextPrimary
        repeat(4) { index ->
            drawOval(
                color = leafColor,
                topLeft = Offset(size.width * (0.10f + index * 0.12f), size.height * (0.12f + index * 0.18f)),
                size = Size(size.width * 0.42f, size.height * 0.16f),
            )
        }
        drawLine(
            color = leafColor,
            start = Offset(size.width * 0.78f, size.height * 0.12f),
            end = Offset(size.width * 0.14f, size.height * 0.92f),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round,
        )
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
                drawOval(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(size.width * 0.50f, size.height * 0.16f),
                    size = Size(size.width * 0.42f, size.height * 0.44f),
                )
                drawOval(
                    color = Color(0xFFF4A3A9).copy(alpha = 0.22f),
                    topLeft = Offset(size.width * 0.56f, size.height * 0.20f),
                    size = Size(size.width * 0.38f, size.height * 0.34f),
                )
                val ribbon = Path().apply {
                    moveTo(size.width * 0.48f, size.height * 0.42f)
                    cubicTo(
                        size.width * 0.60f,
                        size.height * 0.10f,
                        size.width * 0.86f,
                        size.height * 0.16f,
                        size.width * 0.94f,
                        size.height * 0.40f,
                    )
                    cubicTo(
                        size.width * 0.83f,
                        size.height * 0.36f,
                        size.width * 0.71f,
                        size.height * 0.42f,
                        size.width * 0.60f,
                        size.height * 0.58f,
                    )
                    cubicTo(
                        size.width * 0.55f,
                        size.height * 0.54f,
                        size.width * 0.51f,
                        size.height * 0.49f,
                        size.width * 0.48f,
                        size.height * 0.42f,
                    )
                    close()
                }
                drawPath(
                    path = ribbon,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.ribbonStart.copy(alpha = 0.94f),
                            Color.White.copy(alpha = 0.68f),
                            palette.ribbonEnd.copy(alpha = 0.92f),
                        ),
                        start = Offset(size.width * 0.50f, size.height * 0.18f),
                        end = Offset(size.width * 0.88f, size.height * 0.58f),
                    ),
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(size.width * 0.60f, size.height * 0.54f),
                    size = Size(size.width * 0.32f, size.height * 0.08f),
                    cornerRadius = CornerRadius(120f, 120f),
                )
            }

            RankingType.RECOMMEND -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF31B99C).copy(alpha = 0.18f), Color.Transparent),
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.16f),
                    radius = size.width * 0.18f,
                    center = Offset(size.width * 0.58f, size.height * 0.36f),
                    style = Stroke(width = 8f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.width * 0.13f,
                    center = Offset(size.width * 0.56f, size.height * 0.42f),
                    style = Stroke(width = 6f),
                )
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            palette.ribbonStart.copy(alpha = 0.92f),
                            palette.ribbonEnd.copy(alpha = 0.90f),
                        ),
                    ),
                    startAngle = 290f,
                    sweepAngle = 118f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.48f, size.height * 0.08f),
                    size = Size(size.width * 0.42f, size.width * 0.42f),
                    style = Stroke(width = 26f, cap = StrokeCap.Round),
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(size.width * 0.54f, size.height * 0.24f),
                    size = Size(size.width * 0.40f, size.height * 0.10f),
                )
            }

            RankingType.BOOKING -> {
                repeat(6) { index ->
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.08f + index * 0.03f),
                        topLeft = Offset(size.width * 0.52f, size.height * (0.08f + index * 0.07f)),
                        size = Size(size.width * 0.44f, size.height * 0.022f),
                        cornerRadius = CornerRadius(80f, 80f),
                    )
                }
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFCEAB0).copy(alpha = 0.86f),
                            Color(0xFFFF9DA7).copy(alpha = 0.84f),
                        ),
                    ),
                    topLeft = Offset(size.width * 0.60f, size.height * 0.20f),
                    size = Size(size.width * 0.28f, size.height * 0.30f),
                    cornerRadius = CornerRadius(20f, 20f),
                )
                repeat(3) { index ->
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.24f),
                        topLeft = Offset(size.width * (0.61f + index * 0.07f), size.height * 0.24f),
                        size = Size(size.width * 0.03f, size.height * 0.04f),
                        cornerRadius = CornerRadius(8f, 8f),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.24f),
                        topLeft = Offset(size.width * (0.61f + index * 0.07f), size.height * 0.42f),
                        size = Size(size.width * 0.03f, size.height * 0.04f),
                        cornerRadius = CornerRadius(8f, 8f),
                    )
                }
                drawCircle(
                    color = Color.White.copy(alpha = 0.22f),
                    radius = size.width * 0.10f,
                    center = Offset(size.width * 0.74f, size.height * 0.34f),
                    style = Stroke(width = 4f),
                )
                drawPath(
                    path = Path().apply {
                        moveTo(size.width * 0.66f, size.height * 0.22f)
                        lineTo(size.width * 0.66f, size.height * 0.46f)
                        lineTo(size.width * 0.73f, size.height * 0.46f)
                    },
                    color = Color.White.copy(alpha = 0.34f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
private fun RankingContentTypeTabs(
    selected: RankingContentType,
    onSelected: (RankingContentType) -> Unit,
) {
    val tabs = listOf(
        RankingPrimaryTabUiModel(
            text = "全部",
            selected = selected == RankingContentType.ALL,
            onClick = { onSelected(RankingContentType.ALL) },
        ),
        RankingPrimaryTabUiModel(
            text = "真人剧",
            selected = selected == RankingContentType.LIVE_ACTION,
            onClick = { onSelected(RankingContentType.LIVE_ACTION) },
        ),
        RankingPrimaryTabUiModel(text = "漫剧", selected = false, onClick = null),
        RankingPrimaryTabUiModel(
            text = "AI剧",
            selected = selected == RankingContentType.AI,
            onClick = { onSelected(RankingContentType.AI) },
        ),
        RankingPrimaryTabUiModel(text = "演员", selected = false, onClick = null),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 28.dp, end = 28.dp, top = 14.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            Text(
                text = tab.text,
                modifier = if (tab.onClick != null) Modifier.clickable(onClick = tab.onClick) else Modifier,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (tab.selected) FontWeight.Black else FontWeight.Medium,
                    fontSize = 17.sp,
                ),
                color = if (tab.selected) RankingTextPrimary else RankingTextSecondary,
            )
        }
    }
}

@Composable
private fun RankingTypeTabs(
    selected: RankingType,
    onSelected: (RankingType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 28.dp, end = 28.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankingTypePill(
            text = "推荐榜",
            selected = selected == RankingType.RECOMMEND,
            onClick = { onSelected(RankingType.RECOMMEND) },
        )
        RankingTypePill(
            text = "热播榜",
            selected = selected == RankingType.HOT,
            onClick = { onSelected(RankingType.HOT) },
        )
        RankingGhostPill(text = "臻果榜")
        RankingTypePill(
            text = "预约榜",
            selected = selected == RankingType.BOOKING,
            onClick = { onSelected(RankingType.BOOKING) },
        )
        RankingGhostPill(text = "新剧榜")
        RankingGhostPill(text = "热搜榜")
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = RankingCardBackground,
        ) {
            Text(
                text = "分类⌄",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                ),
                color = RankingTextPrimary,
            )
        }
    }
}

@Composable
private fun RankingTypePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) RankingSelectedPill else RankingIdlePill,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                fontSize = 16.sp,
            ),
            color = if (selected) RankingAccentStrong else RankingTextSecondary,
        )
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
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            ),
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
            .background(RankingCardBackground),
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
                    .background(RankingCardBackground.copy(alpha = 0.90f)),
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
        contentPadding = PaddingValues(top = 2.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
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
                modifier = Modifier.padding(horizontal = 20.dp),
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = item.id.isNotBlank(), onClick = onOpenPlay)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RankingPoster(
            item = item,
            modifier = Modifier.size(width = 88.dp, height = 124.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 1.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                        ),
                        color = RankingTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.secondaryText.isNotBlank()) {
                        Text(
                            text = item.secondaryText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                            color = RankingTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                MetricBadge(
                    label = item.metricLabel,
                    value = item.metricValue,
                    visual = item.metricVisual,
                )
            }
            Text(
                text = item.description.ifBlank { "暂无简介" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                ),
                color = RankingTextSecondary,
                maxLines = 1,
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

@Composable
private fun RankingPoster(
    item: RankingDramaItemUiModel,
    modifier: Modifier = Modifier,
) {
    val posterColors = rankingPosterColors(item.posterStyle)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.06f),
                            Color.Black.copy(alpha = 0.30f),
                        ),
                    ),
                ),
        )
        RankBadge(
            rank = item.rank,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            text = item.posterTitle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                lineHeight = 15.sp,
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
                    color = Color.White.copy(alpha = 0.16f),
                    radius = size.width * 0.56f,
                    center = Offset(size.width * 0.82f, size.height * 0.24f),
                )
                drawLine(
                    color = accent.copy(alpha = 0.44f),
                    start = Offset(size.width * 0.14f, size.height * 0.18f),
                    end = Offset(size.width * 0.90f, size.height * 0.64f),
                    strokeWidth = 14f,
                    cap = StrokeCap.Round,
                )
            }

            2 -> {
                repeat(4) { index ->
                    drawRoundRect(
                        color = accent.copy(alpha = 0.12f + index * 0.08f),
                        topLeft = Offset(size.width * (0.10f + index * 0.08f), size.height * (0.16f + index * 0.07f)),
                        size = Size(size.width * 0.78f, size.height * 0.12f),
                        cornerRadius = CornerRadius(24f, 24f),
                    )
                }
            }

            3 -> {
                val path = Path().apply {
                    moveTo(size.width * 0.10f, size.height * 0.28f)
                    quadraticTo(size.width * 0.52f, size.height * 0.08f, size.width * 0.92f, size.height * 0.36f)
                    quadraticTo(size.width * 0.58f, size.height * 0.44f, size.width * 0.20f, size.height * 0.92f)
                    close()
                }
                drawPath(path = path, color = accent.copy(alpha = 0.30f))
            }

            else -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f),
                            accent.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    topLeft = Offset(size.width * 0.10f, size.height * 0.16f),
                    size = Size(size.width * 0.74f, size.height * 0.22f),
                    cornerRadius = CornerRadius(24f, 24f),
                )
            }
        }

        drawCircle(
            color = Color.Black.copy(alpha = 0.10f),
            radius = size.width * 0.56f,
            center = Offset(size.width * 0.80f, size.height * 0.88f),
        )
    }
}

@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    val background = when (rank) {
        1 -> Color(0xFFFFB54A)
        2 -> Color(0xFF14D2B0)
        3 -> Color(0xFF4E97FF)
        else -> Color(0xFF666A77)
    }
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomEnd = 12.dp, bottomStart = 0.dp)

    Surface(
        modifier = modifier,
        shape = shape,
        color = background,
    ) {
        Text(
            text = rank.toString(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun MetricBadge(
    label: String,
    value: String,
    visual: RankingMetricVisual,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Icon(
            imageVector = when (visual) {
                RankingMetricVisual.FLAME -> Icons.Filled.LocalFireDepartment
                RankingMetricVisual.CALENDAR -> Icons.Filled.CalendarMonth
            },
            contentDescription = null,
            tint = RankingAccentStrong,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = value + label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
            ),
            color = RankingAccentStrong,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankingDetailTags(tags: List<RankingDetailTagUiModel>) {
    if (tags.isEmpty()) {
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                    ),
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
        shape = RoundedCornerShape(10.dp),
        color = RankingBookingStrip,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.bookingHintText.orEmpty(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                ),
                color = RankingBookingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onBook,
                enabled = item.id.isNotBlank() && !item.isBooked && !bookingInFlight,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (item.isBooked) RankingBookedButton else RankingBookingButtonBackground,
                    contentColor = if (item.isBooked) RankingTextSecondary else RankingAccentStrong,
                    disabledContainerColor = if (item.isBooked) RankingBookedButton else RankingBookingButtonBackground,
                    disabledContentColor = if (item.isBooked) RankingTextSecondary else RankingAccentStrong,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.heightIn(min = 28.dp),
            ) {
                Text(
                    text = when {
                        bookingInFlight -> "预约中"
                        item.isBooked -> "已预约"
                        else -> "预约"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun RankingLoadingState(isRefreshing: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isRefreshing) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = RankingIdlePill,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = RankingIdlePill,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankingPreviewCard(rank: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 146.dp)
                .clip(RoundedCornerShape(16.dp))
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
            RankingSkeletonLine(width = 0.70f, height = 20.dp)
            RankingSkeletonLine(width = 0.42f, height = 14.dp)
            RankingSkeletonLine(width = 0.92f, height = 16.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RankingSkeletonChip(width = 72.dp)
                RankingSkeletonChip(width = 92.dp)
                RankingSkeletonChip(width = 108.dp)
            }
            RankingSkeletonLine(width = 0.84f, height = 34.dp)
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
            .height(24.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                shape = RoundedCornerShape(18.dp),
                color = RankingIdlePill,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                shape = RoundedCornerShape(18.dp),
                color = RankingIdlePill,
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
                    .padding(vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = RankingTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

internal fun rankingHeaderTopPadding(statusBarInset: Dp): Dp = statusBarInset.coerceAtLeast(18.dp)

private fun rankingBannerTitle(rankingType: RankingType): String = when (rankingType) {
    RankingType.HOT -> "红果热播榜"
    RankingType.RECOMMEND -> "红果推荐榜"
    RankingType.BOOKING -> "红果预约榜"
}

private fun rankingBannerSubtitle(
    contentType: RankingContentType,
    rankingType: RankingType,
): String = when (rankingType) {
    RankingType.HOT -> "7月24日已更新·基于红果内观看/互动等综合热度排序"
    RankingType.RECOMMEND -> "7月24日已更新·基于红果观看/互动以及个人兴趣排序"
    RankingType.BOOKING -> "基于红果预约/播放等综合期待值排序"
}.let { subtitle ->
    if (contentType == RankingContentType.ALL) subtitle else subtitle
}

private data class RankingHeaderPalette(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    val ribbonStart: Color,
    val ribbonEnd: Color,
)

private data class RankingPrimaryTabUiModel(
    val text: String,
    val selected: Boolean,
    val onClick: (() -> Unit)?,
)

private fun rankingHeaderPalette(rankingType: RankingType): RankingHeaderPalette = when (rankingType) {
    RankingType.HOT -> RankingHeaderPalette(
        backgroundStart = Color(0xFFF9D8D2),
        backgroundEnd = Color(0xFFF7CEC8),
        ribbonStart = Color(0xFFF36F63),
        ribbonEnd = Color(0xFFD9E4FF),
    )

    RankingType.RECOMMEND -> RankingHeaderPalette(
        backgroundStart = Color(0xFFBAF0E4),
        backgroundEnd = Color(0xFF9ADCCF),
        ribbonStart = Color(0xFFFFE1A0),
        ribbonEnd = Color(0xFFFFC6B5),
    )

    RankingType.BOOKING -> RankingHeaderPalette(
        backgroundStart = Color(0xFF6FE3F7),
        backgroundEnd = Color(0xFF8DEBFF),
        ribbonStart = Color(0xFFFCE8A7),
        ribbonEnd = Color(0xFFFF95A6),
    )
}

private fun rankingPosterColors(style: RankingPosterStyle): Pair<Color, Color> = when (style) {
    RankingPosterStyle.SUNSET -> Color(0xFF3A312E) to Color(0xFFC88033)
    RankingPosterStyle.EMERALD -> Color(0xFF0E2B2D) to Color(0xFF0FA57D)
    RankingPosterStyle.RIVIERA -> Color(0xFF232743) to Color(0xFF4A8EFA)
    RankingPosterStyle.VIOLET -> Color(0xFF2C224A) to Color(0xFF815BEB)
    RankingPosterStyle.BLUSH -> Color(0xFF4B2F40) to Color(0xFFE283A4)
    RankingPosterStyle.MIDNIGHT -> Color(0xFF1D1E28) to Color(0xFF4A5166)
    RankingPosterStyle.SCARLET -> Color(0xFF4A1E22) to Color(0xFFD44E44)
    RankingPosterStyle.FOREST -> Color(0xFF153329) to Color(0xFF4B966D)
    RankingPosterStyle.SKY -> Color(0xFF7D705A) to Color(0xFFD2C1A0)
    RankingPosterStyle.PLUM -> Color(0xFF3E294C) to Color(0xFFAF7BDE)
    RankingPosterStyle.AMBER -> Color(0xFF4C3520) to Color(0xFFEAAA4E)
    RankingPosterStyle.PEARL -> Color(0xFF7B7F92) to Color(0xFFC6CCDE)
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
    RankingPosterStyle.SKY -> Color(0xFFF6F0DB)
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
        background = Color(0xFFFFF6EA),
        border = Color(0xFFF3DEC5),
        text = Color(0xFFD49B4C),
    )

    RankingDetailTagTone.MINT -> RankingTagPalette(
        background = Color(0xFFE9FFF5),
        border = Color(0xFFC5F0DC),
        text = Color(0xFF0FB679),
    )

    RankingDetailTagTone.CORAL -> RankingTagPalette(
        background = Color(0xFFFFF1EB),
        border = Color(0xFFF6D0BE),
        text = Color(0xFFFF7D2E),
    )
}

private val RankingPageBackground = Color(0xFFFFFFFF)
private val RankingCardBackground = Color(0xFFFFFFFF)
private val RankingTextPrimary = Color(0xFF171717)
private val RankingTextSecondary = Color(0xFF999999)
private val RankingTextMuted = Color(0xFFA7A7A7)
private val RankingAccentStrong = Color(0xFFFF7F1F)
private val RankingSelectedPill = Color(0xFFFFF0E2)
private val RankingIdlePill = Color(0xFFF6F6F6)
private val RankingBookingStrip = Color(0xFFF6F6F6)
private val RankingBookingText = Color(0xFF989898)
private val RankingBookingButtonBackground = Color(0xFFFFF3E8)
private val RankingBookedButton = Color(0xFFE8E8E8)
private val RankingSkeletonBlock = Color(0xFFF2F2F2)
