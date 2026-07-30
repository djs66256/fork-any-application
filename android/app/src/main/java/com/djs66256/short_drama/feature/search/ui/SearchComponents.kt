package com.djs66256.short_drama.feature.search.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djs66256.short_drama.R
import com.djs66256.short_drama.core.theme.SearchCardBorder
import com.djs66256.short_drama.core.theme.SearchCardMeta
import com.djs66256.short_drama.core.theme.SearchDestructive
import com.djs66256.short_drama.core.theme.SearchHistoryText
import com.djs66256.short_drama.core.theme.SearchInputHint
import com.djs66256.short_drama.core.theme.SearchInputIconTint
import com.djs66256.short_drama.core.theme.SearchInputSurface
import com.djs66256.short_drama.core.theme.SearchInputText
import com.djs66256.short_drama.core.theme.SearchPrimaryText
import com.djs66256.short_drama.core.theme.SearchQuickEntryBorder
import com.djs66256.short_drama.core.theme.SearchQuickEntrySurface
import com.djs66256.short_drama.core.theme.SearchSectionTitle
import com.djs66256.short_drama.core.theme.SearchTabSelected
import com.djs66256.short_drama.core.theme.SearchTabSelectedBackground
import com.djs66256.short_drama.core.theme.SearchTabUnselected
import com.djs66256.short_drama.domain.model.HotSearchItem
import com.djs66256.short_drama.domain.model.SearchHistoryItem
import com.djs66256.short_drama.feature.search.model.SearchQuickEntry
import com.djs66256.short_drama.feature.search.model.SearchQuickEntryType

@Composable
fun SearchInputBar(
    query: String,
    isSubmitting: Boolean,
    placeholder: String,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchQueryField(
            query = query,
            placeholder = placeholder,
            onQueryChange = onQueryChange,
            onSubmit = onSubmit,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "搜索",
            modifier = Modifier.clickable(enabled = canSubmitSearch(query) && !isSubmitting, onClick = onSubmit),
            color = if (canSubmitSearch(query) && !isSubmitting) SearchPrimaryText else SearchInputHint,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
            ),
        )
    }
}

@Composable
private fun SearchQueryField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(
            color = SearchInputText,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        ),
        cursorBrush = SolidColor(SearchPrimaryText),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SearchInputSurface)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = SearchInputIconTint,
                    modifier = Modifier.size(20.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isBlank()) {
                        Text(
                            text = placeholder,
                            color = SearchInputHint,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
fun SearchQuickEntrySection(
    entries: List<SearchQuickEntry>,
    onOpenEntry: (SearchQuickEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayEntries = remember(entries) { buildQuickEntryDisplayEntries(entries) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        displayEntries.forEach { entry ->
            val itemModifier = Modifier.width(68.dp)
            if (entry.sourceEntry == null) {
                SearchQuickEntryItem(entry = entry, modifier = itemModifier)
            } else {
                SearchQuickEntryItem(
                    entry = entry,
                    modifier = itemModifier.clickable { onOpenEntry(entry.sourceEntry) },
                )
            }
        }
    }
}

@Composable
private fun SearchQuickEntryItem(
    entry: SearchQuickEntryDisplayEntry,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.shadow(3.dp, RoundedCornerShape(16.dp), clip = false),
            shape = RoundedCornerShape(16.dp),
            color = SearchQuickEntrySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SearchQuickEntryBorder),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 38.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = entry.badgeColor,
                ) {
                    Box(
                        modifier = Modifier.size(26.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = entry.title,
            color = SearchPrimaryText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
fun SearchHistorySection(
    history: List<SearchHistoryItem>,
    onClickHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayHistory = remember(history) { history.map(SearchHistoryItem::keyword).ifEmpty { defaultSearchHistory() } }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.clickable(onClick = onClearHistory),
                color = Color.Transparent,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "清空搜索历史",
                    tint = SearchDestructive,
                    modifier = Modifier
                        .padding(2.dp)
                        .size(24.dp),
                )
            }
        }
        SearchHistoryGrid(
            labels = displayHistory,
            onClick = onClickHistory,
        )
    }
}

@Composable
private fun SearchHistoryGrid(
    labels: List<String>,
    onClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        labels.chunked(2).forEach { rowLabels ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                rowLabels.forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick(label) },
                        color = SearchHistoryText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 30.sp,
                            fontSize = 17.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (rowLabels.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun GuessLikeSection(
    hotSearches: List<HotSearchItem>,
    onClickItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var refreshSeed by rememberSaveable { mutableIntStateOf(0) }
    val cards = remember(hotSearches, refreshSeed) { buildGuessLikeCards(hotSearches, refreshSeed) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(title = "猜你想搜")
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "刷新猜你想搜",
                tint = SearchPrimaryText,
                modifier = Modifier
                    .size(30.dp)
                    .clickable { refreshSeed += 1 },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cards.forEach { card ->
                GuessLikeCard(
                    card = card,
                    modifier = Modifier.width(142.dp),
                    onClick = { onClickItem(card.keyword) },
                )
            }
        }
    }
}

@Composable
private fun GuessLikeCard(
    card: SearchPosterCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(id = card.imageRes),
            contentDescription = card.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.FillBounds,
        )
        Text(
            text = card.title,
            color = SearchPrimaryText,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = card.meta,
            color = SearchCardMeta,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun HotSearchSection(
    hotSearches: List<HotSearchItem>,
    isLoading: Boolean,
    errorMessage: String?,
    onClickHotSearch: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember(hotSearches) { buildHotRankingTabs(hotSearches) }
    var selectedTabIndex by rememberSaveable(tabs) { mutableIntStateOf(0) }
    val safeIndex = selectedTabIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SearchHotTabs(
            tabs = tabs,
            selectedTabIndex = safeIndex,
            onSelected = { selectedTabIndex = it },
        )
        if ((errorMessage != null || isLoading) && hotSearches.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth())
        }
        HotSearchRankingGrid(
            items = tabs[safeIndex].items,
            onClickHotSearch = onClickHotSearch,
        )
    }
}

@Composable
private fun SearchHotTabs(
    tabs: List<SearchHotTab>,
    selectedTabIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            Surface(
                modifier = Modifier.clickable { onSelected(index) },
                color = if (index == selectedTabIndex) SearchTabSelectedBackground else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = tab.title,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (index == selectedTabIndex) SearchTabSelected else SearchTabUnselected,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (index == selectedTabIndex) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HotSearchRankingGrid(
    items: List<SearchRankingCard>,
    onClickHotSearch: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { item ->
                    HotSearchRankingCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = { onClickHotSearch(item.keyword) },
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HotSearchRankingCard(
    item: SearchRankingCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(id = item.imageRes),
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.FillBounds,
        )
        Text(
            text = item.title,
            color = SearchPrimaryText,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.subTitle,
            color = SearchCardMeta,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        text = title,
        color = SearchSectionTitle,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
        ),
    )
}

@Composable
private fun EmptyStateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = SearchCardMeta,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChipRow(
    labels: List<String>,
    onClick: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            Surface(
                modifier = Modifier.clickable { onClick(label) },
                shape = RoundedCornerShape(20.dp),
                color = SearchInputSurface,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SearchHistoryText,
                )
            }
        }
    }
}

internal fun canSubmitSearch(query: String): Boolean {
    return com.djs66256.short_drama.domain.model.normalizeSearchQueryOrNull(query) != null
}

@Composable
fun SearchResultEmptyState(
    query: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (query.isBlank()) "暂无搜索结果" else "未找到与“$query”相关的短剧",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SearchResultErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = Modifier.clickable(onClick = onRetry),
            shape = RoundedCornerShape(20.dp),
            color = SearchInputSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SearchCardBorder),
        ) {
            Text(
                text = "重试",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                color = SearchPrimaryText,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun buildQuickEntryDisplayEntries(entries: List<SearchQuickEntry>): List<SearchQuickEntryDisplayEntry> {
    val actualEntries = entries.associateBy(SearchQuickEntry::type)
    return listOf(
        SearchQuickEntryDisplayEntry(
            title = "识剧",
            icon = Icons.Filled.CameraAlt,
            badgeColor = Color(0xFF22C7D8),
            sourceEntry = null,
        ),
        SearchQuickEntryDisplayEntry(
            title = "排行",
            icon = Icons.Filled.LocalFireDepartment,
            badgeColor = Color(0xFFFF8B2D),
            sourceEntry = actualEntries[SearchQuickEntryType.RANKING],
        ),
        SearchQuickEntryDisplayEntry(
            title = "上新",
            icon = Icons.Filled.PlayCircle,
            badgeColor = Color(0xFF1FC7C9),
            sourceEntry = actualEntries[SearchQuickEntryType.NEW_RELEASES],
        ),
        SearchQuickEntryDisplayEntry(
            title = "演员",
            icon = Icons.Filled.Person,
            badgeColor = Color(0xFFF3B24E),
            sourceEntry = actualEntries[SearchQuickEntryType.ACTORS],
        ),
        SearchQuickEntryDisplayEntry(
            title = "分类",
            icon = Icons.Filled.Apps,
            badgeColor = Color(0xFF9B7CF7),
            sourceEntry = actualEntries[SearchQuickEntryType.CLASSIFICATION],
        ),
    )
}

private fun defaultSearchHistory(): List<String> = listOf(
    "求生",
    "异界",
    "系统",
    "都市日常",
    "我在废土世界种草莓",
    "青春甜宠",
)

private fun buildGuessLikeCards(
    hotSearches: List<HotSearchItem>,
    refreshSeed: Int,
): List<SearchPosterCard> {
    val fallbackKeywords = listOf("末日求生", "都市甜宠", "仙侠逆袭")
    val sourceKeywords = hotSearches.map(HotSearchItem::keyword).ifEmpty { fallbackKeywords }
    val metas = listOf("剧情 · 全124集", "甜宠 · 全81集", "玄幻脑洞 · 全60集")
    val imageRes = listOf(
        R.drawable.search_hot_1,
        R.drawable.search_hot_2,
        R.drawable.search_hot_3,
    )

    return List(imageRes.size) { index ->
        val keyword = sourceKeywords[(index + refreshSeed) % sourceKeywords.size]
        SearchPosterCard(
            imageRes = imageRes[(index + refreshSeed) % imageRes.size],
            keyword = keyword,
            title = fallbackKeywords[index],
            meta = metas[index],
        )
    }
}

private fun buildHotRankingTabs(hotSearches: List<HotSearchItem>): List<SearchHotTab> {
    val sourceItems = hotSearches.ifEmpty {
        listOf(
            HotSearchItem(rank = 1, keyword = "我靠奶奶的金项链", score = 7_960_000),
            HotSearchItem(rank = 2, keyword = "枭雄崛起，从打工开始", score = 6_030_000),
            HotSearchItem(rank = 3, keyword = "一起捉迷藏", score = 3_970_000),
            HotSearchItem(rank = 4, keyword = "此去经年", score = 3_210_000),
            HotSearchItem(rank = 5, keyword = "旧梦惊鸿", score = 2_800_000),
            HotSearchItem(rank = 6, keyword = "迷局深宫", score = 2_560_000),
        )
    }
    val titles = listOf("热门热搜榜", "漫剧热搜榜", "预约榜", "热点话题榜")
    return titles.mapIndexed { index, title ->
        SearchHotTab(
            title = title,
            items = buildRankingCardsForTab(sourceItems, shift = index),
        )
    }
}

private fun buildRankingCardsForTab(
    hotSearches: List<HotSearchItem>,
    shift: Int,
): List<SearchRankingCard> {
    val imageRes = listOf(
        R.drawable.search_hot_1,
        R.drawable.search_hot_2,
        R.drawable.search_hot_3,
        R.drawable.search_hot_4,
        R.drawable.search_hot_5,
        R.drawable.search_hot_6,
    )
    return List(minOf(6, hotSearches.size)) { index ->
        val item = hotSearches[(index + shift) % hotSearches.size]
        SearchRankingCard(
            imageRes = imageRes[index % imageRes.size],
            keyword = item.keyword,
            title = item.keyword,
            subTitle = "${formatSearchScore(item.score)}热搜值",
        )
    }
}

private fun formatSearchScore(score: Int): String {
    return when {
        score >= 10_000_000 -> "${score / 10_000_000}亿"
        score >= 10_000 -> "${score / 10_000}万"
        else -> score.toString()
    }
}

private data class SearchQuickEntryDisplayEntry(
    val title: String,
    val icon: ImageVector,
    val badgeColor: Color,
    val sourceEntry: SearchQuickEntry?,
)

private data class SearchPosterCard(
    @DrawableRes val imageRes: Int,
    val keyword: String,
    val title: String,
    val meta: String,
)

private data class SearchHotTab(
    val title: String,
    val items: List<SearchRankingCard>,
)

private data class SearchRankingCard(
    @DrawableRes val imageRes: Int,
    val keyword: String,
    val title: String,
    val subTitle: String,
)
