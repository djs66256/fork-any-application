package com.djs66256.short_drama.feature.theater.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djs66256.short_drama.R
import com.djs66256.short_drama.core.theme.TheaterCardBorder
import com.djs66256.short_drama.core.theme.TheaterCardSurface
import com.djs66256.short_drama.core.theme.TheaterHeatScrim
import com.djs66256.short_drama.core.theme.TheaterHeatText
import com.djs66256.short_drama.core.theme.TheaterNeutralChipBackground
import com.djs66256.short_drama.core.theme.TheaterNeutralChipText
import com.djs66256.short_drama.core.theme.TheaterPageBackground
import com.djs66256.short_drama.core.theme.TheaterPrimaryText
import com.djs66256.short_drama.core.theme.TheaterSearchDivider
import com.djs66256.short_drama.core.theme.TheaterSecondaryText
import com.djs66256.short_drama.core.theme.TheaterSelectedTabIndicatorEnd
import com.djs66256.short_drama.core.theme.TheaterSelectedTabIndicatorStart
import com.djs66256.short_drama.core.theme.TheaterSelectedTabText
import com.djs66256.short_drama.core.theme.TheaterTopSearchSurface
import com.djs66256.short_drama.core.theme.TheaterUnselectedTabText
import com.djs66256.short_drama.core.theme.TheaterWarmChipBackground
import com.djs66256.short_drama.core.theme.TheaterWarmChipText
import com.djs66256.short_drama.domain.model.TheaterChannel
import com.djs66256.short_drama.feature.theater.model.TheaterDramaItemUiModel
import com.djs66256.short_drama.feature.theater.viewmodel.TheaterUiState
import com.djs66256.short_drama.navigation.TheaterShortcutRoute
import kotlin.math.absoluteValue

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
    val showFallbackCards =
        uiState.items.isEmpty() &&
            uiState.selectedChannel == TheaterChannel.ALL &&
            !uiState.isLoading
    val fallbackCards = theaterFallbackCards()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .background(TheaterPageBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
            TheaterShortcutRow(onShortcutClick = onShortcutClick)
        }

        when {
            showFallbackCards -> {
                items(
                    count = fallbackCards.size,
                    key = { index -> fallbackCards[index].item.id },
                ) { index ->
                    val card = fallbackCards[index]
                    TheaterDramaCard(
                        item = card.item,
                        fallbackPosterResId = card.posterResId,
                        onOpenPlay = { onDramaClick(card.item.id) },
                    )
                }
            }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(TheaterTopSearchSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSearchClick)
                    .padding(start = 14.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = TheaterSecondaryText,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "一剑镇狱第二季",
                    color = TheaterSecondaryText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 1.dp, height = 24.dp)
                    .background(TheaterSearchDivider),
            )
            Row(
                modifier = Modifier
                    .clickable(onClick = onScanClick)
                    .padding(start = 12.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "识剧入口",
                    tint = TheaterSecondaryText,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "识剧",
                    color = TheaterSecondaryText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                )
            }
        }
    }
}

@Composable
fun TheaterChannelTabs(
    selectedChannel: TheaterChannel,
    onChannelSelected: (TheaterChannel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(25.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TheaterChannel.entries.forEach { channel ->
            val selected = channel == selectedChannel
            Column(
                modifier = Modifier.clickable { onChannelSelected(channel) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = channel.uiLabel(),
                    color = if (selected) TheaterSelectedTabText else TheaterUnselectedTabText,
                    fontSize = if (selected) 18.sp else 17.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        TheaterSelectedTabIndicatorStart,
                                        TheaterSelectedTabIndicatorEnd,
                                    ),
                                )
                            } else {
                                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                        ),
                )
            }
        }
    }
}

@Composable
fun TheaterShortcutRow(
    onShortcutClick: (TheaterShortcutRoute) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TheaterShortcutRoute.quickEntries.forEach { shortcut ->
            TheaterShortcutCard(
                shortcut = shortcut,
                onClick = { onShortcutClick(shortcut) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TheaterShortcutCard(
    shortcut: TheaterShortcutRoute,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = shortcut.accentColor()
    Surface(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = TheaterCardSurface,
        border = BorderStroke(1.dp, TheaterCardBorder),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(accent.copy(alpha = 0.92f), accent.copy(alpha = 0.66f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = shortcut.icon(),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = shortcut.uiTitle(),
                color = TheaterPrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TheaterDramaCard(
    item: TheaterDramaItemUiModel,
    onOpenPlay: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes fallbackPosterResId: Int? = null,
) {
    val palette = posterPaletteFor(item)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = item.id.isNotBlank(), onClick = onOpenPlay),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TheaterCardSurface),
        border = BorderStroke(1.dp, TheaterCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(colors = palette.background)),
            ) {
                if (fallbackPosterResId != null) {
                    Image(
                        painter = painterResource(id = fallbackPosterResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    GradientPosterBackdrop(item = item, palette = palette)
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0x40000000),
                                    Color(0xB5000000),
                                ),
                            ),
                        ),
                )
                item.badgeText?.let { badgeText ->
                    TheaterPosterBadge(
                        text = badgeText,
                        color = palette.badgeColor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                    )
                }
                Text(
                    text = item.title,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, end = 12.dp, bottom = 34.dp),
                    color = Color.White,
                    style = TextStyle(
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.34f),
                            blurRadius = 10f,
                        ),
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                TheaterHeatChip(
                    text = "${item.heatText}热度",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 10.dp),
                )
            }
            Text(
                text = item.title,
                modifier = Modifier.padding(horizontal = 6.dp),
                color = TheaterPrimaryText,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item.chipTexts.forEachIndexed { index, chip ->
                    TheaterDramaChip(
                        text = chip,
                        emphasized = index == 0,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun GradientPosterBackdrop(
    item: TheaterDramaItemUiModel,
    palette: TheaterPosterPalette,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(126.dp)
                .offset(x = (-18).dp, y = (-12).dp)
                .clip(CircleShape)
                .background(palette.glowStart.copy(alpha = 0.48f)),
        )
        Box(
            modifier = Modifier
                .size(164.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 40.dp, y = (-8).dp)
                .clip(CircleShape)
                .background(palette.glowEnd.copy(alpha = 0.24f)),
        )
        Box(
            modifier = Modifier
                .size(width = 110.dp, height = 180.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 10.dp)
                .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 18.dp, bottomStart = 60.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        if (item.category.isNotBlank()) {
            Text(
                text = item.category,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp),
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun TheaterPosterBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 4.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TheaterHeatChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TheaterHeatScrim)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = TheaterHeatText,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = text,
            color = TheaterHeatText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TheaterDramaChip(
    text: String,
    emphasized: Boolean,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (emphasized) TheaterWarmChipBackground else TheaterNeutralChipBackground)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            color = if (emphasized) TheaterWarmChipText else TheaterNeutralChipText,
            fontSize = 12.sp,
            fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TheaterLoadingState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 42.dp, bottom = 28.dp),
        shape = RoundedCornerShape(20.dp),
        color = TheaterCardSurface,
        border = BorderStroke(1.dp, TheaterCardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = TheaterSelectedTabText)
            Text(
                text = "正在整理剧场内容...",
                modifier = Modifier.padding(top = 12.dp),
                color = TheaterSecondaryText,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun TheaterEmptyState(channel: TheaterChannel) {
    val title = if (channel == TheaterChannel.ALL) "暂时没有可展示内容" else "${channel.uiLabel()}频道筹备中"
    val description = if (channel == TheaterChannel.ALL) {
        "稍后再来看看新的短剧推荐。"
    } else {
        "当前先保留轻量占位，后续会补齐该频道内容。"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 28.dp),
        shape = RoundedCornerShape(20.dp),
        color = TheaterCardSurface,
        border = BorderStroke(1.dp, TheaterCardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = TheaterPrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 8.dp),
                color = TheaterSecondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TheaterErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 28.dp),
        shape = RoundedCornerShape(20.dp),
        color = TheaterCardSurface,
        border = BorderStroke(1.dp, TheaterCardBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "加载失败",
                color = TheaterPrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                modifier = Modifier.padding(top = 8.dp),
                color = TheaterSecondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text("重试")
            }
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
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = TheaterSelectedTabText,
                )
                Text(
                    text = "正在加载更多...",
                    modifier = Modifier.padding(start = 8.dp),
                    color = TheaterSecondaryText,
                    fontSize = 14.sp,
                )
            }
        }
        appendErrorMessage != null && hasItems -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = appendErrorMessage,
                    color = TheaterSecondaryText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(onClick = onRetry) {
                    Text("重试加载更多")
                }
            }
        }
        !hasNextPage && hasItems -> {
            Text(
                text = "已经到底啦",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                color = TheaterSecondaryText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun TheaterChannel.uiLabel(): String = when (this) {
    TheaterChannel.ALL -> "找剧"
    TheaterChannel.REAL -> "真人剧"
    TheaterChannel.ANIME -> "漫剧"
    TheaterChannel.MOVIE -> "电影"
    TheaterChannel.AUDIO -> "听书"
    TheaterChannel.NOVEL -> "小说"
    TheaterChannel.COMIC -> "漫画"
    TheaterChannel.BIGSCREEN -> "大视听"
}

private fun TheaterShortcutRoute.uiTitle(): String = when (this) {
    TheaterShortcutRoute.Ranking -> "排行榜"
    else -> title
}

private fun TheaterShortcutRoute.icon(): ImageVector = when (this) {
    TheaterShortcutRoute.Classification -> Icons.Filled.FilterAlt
    TheaterShortcutRoute.Ranking -> Icons.Filled.LocalFireDepartment
    TheaterShortcutRoute.NewReleases -> Icons.Filled.PlayCircleFilled
    TheaterShortcutRoute.Booking -> Icons.Filled.CalendarMonth
    TheaterShortcutRoute.Search -> Icons.Filled.Search
}

private fun TheaterShortcutRoute.accentColor(): Color = when (this) {
    TheaterShortcutRoute.Classification -> Color(0xFF9B82F4)
    TheaterShortcutRoute.Ranking -> Color(0xFFFF7B22)
    TheaterShortcutRoute.NewReleases -> Color(0xFF19C6C8)
    TheaterShortcutRoute.Booking -> Color(0xFFF2B23A)
    TheaterShortcutRoute.Search -> Color(0xFF7A7A7A)
}

private data class TheaterPosterPalette(
    val background: List<Color>,
    val glowStart: Color,
    val glowEnd: Color,
    val badgeColor: Color,
)

private data class TheaterFallbackCard(
    val item: TheaterDramaItemUiModel,
    @DrawableRes val posterResId: Int,
)

private fun theaterFallbackCards(): List<TheaterFallbackCard> = listOf(
    TheaterFallbackCard(
        item = TheaterDramaItemUiModel(
            id = "fallback-1",
            title = "咱家剑宗团宠小师妹",
            description = "",
            coverUrl = "",
            category = "",
            chipTexts = listOf("热播榜 No.9", "经典漫剧"),
            badgeText = "爆剧",
            heatText = "6126万",
        ),
        posterResId = R.drawable.theater_poster_1,
    ),
    TheaterFallbackCard(
        item = TheaterDramaItemUiModel(
            id = "fallback-2",
            title = "副本老大是男友",
            description = "",
            coverUrl = "",
            category = "",
            chipTexts = listOf("AI剧收藏榜 No.8", "恋爱"),
            badgeText = null,
            heatText = "4400万",
        ),
        posterResId = R.drawable.theater_poster_2,
    ),
    TheaterFallbackCard(
        item = TheaterDramaItemUiModel(
            id = "fallback-3",
            title = "昼以继夜2",
            description = "",
            coverUrl = "",
            category = "",
            chipTexts = listOf("最高热度破9000万", "爱情"),
            badgeText = "新剧",
            heatText = "6512万",
        ),
        posterResId = R.drawable.theater_poster_3,
    ),
    TheaterFallbackCard(
        item = TheaterDramaItemUiModel(
            id = "fallback-4",
            title = "野路子·第一季",
            description = "",
            coverUrl = "",
            category = "",
            chipTexts = listOf("动作冒险", "动作打斗"),
            badgeText = "红果首发",
            heatText = "369万",
        ),
        posterResId = R.drawable.theater_poster_4,
    ),
    TheaterFallbackCard(
        item = TheaterDramaItemUiModel(
            id = "fallback-5",
            title = "隐婚老公太会宠",
            description = "",
            coverUrl = "",
            category = "",
            chipTexts = listOf("甜宠天花板", "总裁"),
            badgeText = "热播",
            heatText = "7211万",
        ),
        posterResId = R.drawable.theater_poster_2,
    ),
    TheaterFallbackCard(
        item = TheaterDramaItemUiModel(
            id = "fallback-6",
            title = "重生后我把渣男送进火葬场",
            description = "",
            coverUrl = "",
            category = "",
            chipTexts = listOf("复仇爽感", "重生"),
            badgeText = "爆款",
            heatText = "6890万",
        ),
        posterResId = R.drawable.theater_poster_1,
    ),
)

private fun posterPaletteFor(item: TheaterDramaItemUiModel): TheaterPosterPalette {
    val palettes = listOf(
        TheaterPosterPalette(
            background = listOf(Color(0xFF4B566E), Color(0xFF2F3342), Color(0xFF17171D)),
            glowStart = Color(0xFFD6E1FF),
            glowEnd = Color(0xFFB6C2FF),
            badgeColor = Color(0xFFFF6B66),
        ),
        TheaterPosterPalette(
            background = listOf(Color(0xFF5A3A43), Color(0xFF2E1D27), Color(0xFF151317)),
            glowStart = Color(0xFFFFD5E0),
            glowEnd = Color(0xFFF4B8C8),
            badgeColor = Color(0xFFFF8A2A),
        ),
        TheaterPosterPalette(
            background = listOf(Color(0xFF87746A), Color(0xFF564640), Color(0xFF201C1A)),
            glowStart = Color(0xFFFFF2E0),
            glowEnd = Color(0xFFE8D8CB),
            badgeColor = Color(0xFF14B8A6),
        ),
        TheaterPosterPalette(
            background = listOf(Color(0xFF5F4A2C), Color(0xFF3E2E1B), Color(0xFF191412)),
            glowStart = Color(0xFFFFE2AD),
            glowEnd = Color(0xFFFFC570),
            badgeColor = Color(0xFFFF9800),
        ),
    )
    return palettes[item.id.hashCode().absoluteValue % palettes.size]
}

private fun LazyGridScope.fullSpanItem(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        content()
    }
}
