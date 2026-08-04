package com.djs66256.short_drama.feature.home.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.djs66256.short_drama.core.theme.HomeFeedAccent
import com.djs66256.short_drama.core.theme.HomeFeedAccentSoft
import com.djs66256.short_drama.core.theme.HomeFeedAccentStrong
import com.djs66256.short_drama.core.theme.HomeFeedBadge
import com.djs66256.short_drama.core.theme.HomeFeedCardBottom
import com.djs66256.short_drama.core.theme.HomeFeedCardMiddle
import com.djs66256.short_drama.core.theme.HomeFeedCardTop
import com.djs66256.short_drama.core.theme.HomeFeedChip
import com.djs66256.short_drama.core.theme.HomeFeedChipText
import com.djs66256.short_drama.core.theme.HomeFeedFrameCtaBorder
import com.djs66256.short_drama.core.theme.HomeFeedFrameCtaSurface
import com.djs66256.short_drama.core.theme.HomeFeedMetaBar
import com.djs66256.short_drama.core.theme.HomeFeedMutedText
import com.djs66256.short_drama.core.theme.HomeFeedRailSurface
import com.djs66256.short_drama.core.theme.HomeFeedRailText
import com.djs66256.short_drama.core.theme.HomeFeedScrim
import com.djs66256.short_drama.core.theme.HomeFeedTopBarIconBorder
import com.djs66256.short_drama.core.theme.HomeFeedTopBarIconSurface
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.feature.comments.model.CommentLoginContext
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.ui.CommentBottomSheet
import com.djs66256.short_drama.feature.comments.ui.CommentLoginPlaceholderDialog
import com.djs66256.short_drama.feature.home.viewmodel.HomeViewModel
import com.djs66256.short_drama.feature.player.player.PlaceholderPlayerHost
import com.djs66256.short_drama.feature.player.player.PlayerEventAdapter
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPlay: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeCommentDramaId by remember { mutableStateOf<String?>(null) }
    var pendingCommentLoginContext by remember { mutableStateOf<CommentLoginContext?>(null) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { uiState.items.size },
    )

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onForegrounded()
                Lifecycle.Event.ON_STOP -> viewModel.onBackgrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenDisposed()
        }
    }

    LaunchedEffect(pagerState, uiState.items) {
        kotlinx.coroutines.flow.snapshotFlow { currentHomeFeedDrama(uiState.items, pagerState.settledPage)?.id }
            .map { it?.takeIf(String::isNotBlank) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect(viewModel::onVisibleDramaChanged)
    }

    val errorMessage = uiState.errorMessage
    val activePage = pagerState.settledPage

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.isLoading -> HomeFeedLoadingState(isRetrying = uiState.isRetrying)
            errorMessage != null -> HomeFeedErrorState(
                message = errorMessage,
                onRetry = viewModel::retry,
            )
            uiState.items.isEmpty() -> HomeFeedEmptyState()
            else -> VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> uiState.items[page].id },
            ) { page ->
                val drama = uiState.items[page]
                val pagePlayerState = activePlayerStateForDrama(
                    dramaId = drama.id,
                    currentPage = activePage,
                    page = page,
                    activeDramaId = uiState.activeDramaId,
                    activePlayerUiState = uiState.activePlayerUiState,
                )
                HomeVideoFeedPage(
                    drama = drama,
                    playerUiState = pagePlayerState,
                    onPlay = { onOpenPlay(drama.id) },
                    onDetail = { onOpenDetail(drama.id) },
                    onComment = { activeCommentDramaId = drama.id },
                    onPlaybackPositionChanged = viewModel::onFeedPlaybackPositionChanged,
                    onPlaybackError = viewModel::onFeedPlaybackError,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        HomeTopBar(
            onOpenMenu = onOpenMenu,
            onOpenSearch = onOpenSearch,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 2.dp),
        )

        val hasBlockingModal = activeCommentDramaId != null || pendingCommentLoginContext != null
        if (hasBlockingModal) {
            LaunchedEffect(hasBlockingModal) {
                viewModel.abandonCheckInPopupForCurrentSession()
            }
        }

        val featuredDrama = currentHomeFeedDrama(uiState.items, activePage)
        if (featuredDrama != null && !uiState.isLoading && errorMessage == null) {
            HomeFrameCta(
                episodeCount = featuredDrama.episodeCount,
                onClick = {
                    if (hasNavigableDramaId(featuredDrama.id)) {
                        onOpenPlay(featuredDrama.id)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        if (shouldRenderCheckInPopup(uiState.checkInPopup.isVisible, hasBlockingModal)) {
            CheckInPopup(
                state = uiState.checkInPopup,
                onClose = viewModel::dismissCheckInPopup,
                onSubmit = viewModel::submitCheckIn,
            )
        }
    }

    activeCommentDramaId?.let { dramaId ->
        CommentBottomSheet(
            dramaId = dramaId,
            source = CommentSource.HOME,
            onDismiss = { activeCommentDramaId = null },
            onRequireLogin = { effect ->
                activeCommentDramaId = null
                pendingCommentLoginContext = effect.context
                Toast.makeText(context, "请先登录后再操作评论", Toast.LENGTH_SHORT).show()
            },
            onMessage = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }

    pendingCommentLoginContext?.let { loginContext ->
        CommentLoginPlaceholderDialog(
            context = loginContext,
            onConfirmLogin = {
                activeCommentDramaId = loginContext.dramaId
                pendingCommentLoginContext = null
                Toast.makeText(context, "已回到评论抽屉，请手动重新执行操作", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                pendingCommentLoginContext = null
            },
        )
    }
}

@Composable
private fun HomeVideoFeedPage(
    drama: Drama,
    playerUiState: PlayerUiState?,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    onComment: () -> Unit,
    onPlaybackPositionChanged: (Double) -> Unit,
    onPlaybackError: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = homeFeedBottomContentPadding(),
) {
    val actionsEnabled = hasNavigableDramaId(drama.id)
    val interactionItems = remember(drama) { buildInteractionItems(drama) }
    val infoBadge = remember(drama) { buildDramaInfoBadge(drama) }
    val titleTags = drama.tags.take(4)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (playerUiState != null) {
            PlaceholderPlayerHost(
                uiState = playerUiState,
                eventAdapter = PlayerEventAdapter(
                    onPositionChanged = onPlaybackPositionChanged,
                    onPlaybackError = onPlaybackError,
                ),
                modifier = Modifier.fillMaxSize(),
            )
            HomeFeedPlayerStatusOverlay(
                uiState = playerUiState,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            DramaCoverPlaceholder(
                drama = drama,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x33000000),
                            HomeFeedScrim,
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = bottomContentPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (infoBadge.isNotBlank()) {
                    InfoBadge(text = infoBadge)
                }

                Text(
                    text = drama.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (titleTags.isNotEmpty()) {
                    FlowTagRow(tags = titleTags)
                }

                Text(
                    text = drama.description.ifBlank { "暂无简介" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = HomeFeedMutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = buildDramaMeta(drama),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomeFeedMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                HomeCardFooter(
                    enabled = actionsEnabled,
                    episodeCount = drama.episodeCount,
                    onClick = {
                        if (actionsEnabled) {
                            onPlay()
                        }
                    },
                    onDetail = {
                        if (actionsEnabled) {
                            onDetail()
                        }
                    },
                )
            }

            InteractionRail(
                items = interactionItems,
                onComment = {
                    if (actionsEnabled) {
                        onComment()
                    }
                },
                enabled = actionsEnabled,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun HomeFeedPlayerStatusOverlay(
    uiState: PlayerUiState,
    modifier: Modifier = Modifier,
) {
    val statusCopy = homeFeedPlayerStatusCopy(uiState) ?: return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.Black.copy(alpha = 0.56f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.screenState == PlayerScreenState.BOOTSTRAPPING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            }
            Text(
                text = statusCopy,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeTopBarIconButton(
            imageVector = Icons.Filled.Menu,
            contentDescription = HOME_MENU_ENTRY_CONTENT_DESCRIPTION,
            onClick = onOpenMenu,
        )
        HomeTopBarIconButton(
            imageVector = Icons.Filled.Search,
            contentDescription = HOME_SEARCH_ENTRY_CONTENT_DESCRIPTION,
            onClick = onOpenSearch,
        )
    }
}

@Composable
private fun HomeTopBarIconButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = HomeFeedTopBarIconSurface,
        border = BorderStroke(1.dp, HomeFeedTopBarIconBorder),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun HomeFeedLoadingState(isRetrying: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = HomeFeedAccent)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRetrying) "正在重新加载首页内容..." else "正在加载首页内容...",
            style = MaterialTheme.typography.bodyLarge,
            color = HomeFeedMutedText,
        )
    }
}

@Composable
private fun HomeFeedEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "暂无内容",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前还没有可展示的短剧，稍后再来看看。",
            style = MaterialTheme.typography.bodyMedium,
            color = HomeFeedMutedText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeFeedErrorState(
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
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = HomeFeedMutedText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeFeedAccentStrong,
                contentColor = Color.White,
            ),
        ) {
            Text("重试")
        }
    }
}

@Composable
fun HomeDramaCard(
    drama: Drama,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    onComment: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = homeFeedBottomContentPadding(),
) {
    HomeVideoFeedPage(
        drama = drama,
        playerUiState = null,
        onPlay = onPlay,
        onDetail = onDetail,
        onComment = onComment,
        onPlaybackPositionChanged = {},
        onPlaybackError = {},
        modifier = modifier,
        bottomContentPadding = bottomContentPadding,
    )
}

@Composable
private fun DramaCoverPlaceholder(
    drama: Drama,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        HomeFeedCardTop,
                        HomeFeedCardMiddle,
                        HomeFeedCardBottom,
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E140D),
                            Color(0xFF3A2413),
                            HomeFeedAccentStrong,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0x66000000),
                            Color(0xCC000000),
                        ),
                    ),
                ),
        )
        Text(
            text = buildCoverWatermark(drama),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 32.dp, end = 18.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.78f),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, end = 96.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = HomeFeedMetaBar,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = if (drama.coverUrl.isBlank()) "静态封面占位" else "短剧封面",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
            Text(
                text = drama.category.ifBlank { "热门推荐" },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InfoBadge(text: String) {
    Surface(
        color = HomeFeedMetaBar,
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun FlowTagRow(tags: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            Surface(
                color = HomeFeedChip,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = HomeFeedChipText,
                )
            }
        }
    }
}

@Composable
private fun InteractionRail(
    items: List<HomeInteractionItem>,
    onComment: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(72.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = HomeFeedRailSurface,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    onClick = {
                        if (item.isComment) {
                            onComment()
                        }
                    },
                    enabled = enabled || !item.isComment,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = item.tint,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Text(
                    text = item.countLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = HomeFeedRailText,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun HomeCardFooter(
    enabled: Boolean,
    episodeCount: Int,
    onClick: () -> Unit,
    onDetail: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "观看完整漫剧 · 全${episodeCount.coerceAtLeast(1)}集",
            color = HomeFeedMutedText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            onClick = onDetail,
            enabled = enabled,
        ) {
            Text(
                text = "详情 >",
                color = HomeFeedMutedText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeFeedAccentStrong,
                contentColor = Color.White,
                disabledContainerColor = HomeFeedAccentStrong.copy(alpha = 0.35f),
                disabledContentColor = Color.White.copy(alpha = 0.6f),
            ),
        ) {
            Text("去看")
        }
    }
}

@Composable
internal fun HomeFrameCta(
    episodeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        color = HomeFeedFrameCtaSurface,
        border = BorderStroke(1.dp, HomeFeedFrameCtaBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(10.dp),
                color = HomeFeedBadge,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = HomeFeedAccentSoft,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = buildFrameCtaTitle(episodeCount),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "首页主入口",
                    color = Color.White.copy(alpha = 0.42f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeFeedAccentStrong,
                    contentColor = Color.White,
                ),
            ) {
                Text("去看")
            }
        }
    }
}

private data class HomeInteractionItem(
    val label: String,
    val countLabel: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val isComment: Boolean = false,
)

private fun buildInteractionItems(drama: Drama): List<HomeInteractionItem> {
    val ratingBase = drama.rating.coerceAtLeast(0.0)
    val favoriteCount = ((drama.episodeCount + 3) * 3_600).coerceAtLeast(3_294)
    val commentCount = ((ratingBase * 55).toInt() + drama.tags.size * 18).coerceAtLeast(470)
    val likeCount = (favoriteCount / 3).coerceAtLeast(4_485)
    val shareCount = (commentCount * 9 + 314).coerceAtLeast(1_024)

    return listOf(
        HomeInteractionItem(
            label = "收藏",
            countLabel = formatCompactCount(favoriteCount),
            icon = Icons.Filled.StarBorder,
            tint = HomeFeedAccentSoft,
        ),
        HomeInteractionItem(
            label = "评论",
            countLabel = formatCompactCount(commentCount),
            icon = Icons.Filled.ChatBubble,
            tint = Color.White,
            isComment = true,
        ),
        HomeInteractionItem(
            label = "点赞",
            countLabel = formatCompactCount(likeCount),
            icon = Icons.Filled.FavoriteBorder,
            tint = Color.White,
        ),
        HomeInteractionItem(
            label = "分享",
            countLabel = formatCompactCount(shareCount),
            icon = Icons.Filled.Share,
            tint = Color.White,
        ),
    )
}

private fun buildDramaInfoBadge(drama: Drama): String {
    val category = drama.category.ifBlank { "热门短剧" }
    val target = (drama.rating * 100_000).toInt().coerceAtLeast(400_000)
    return "$category | 共${formatCompactCount(target)}人在追"
}

private fun buildCoverWatermark(drama: Drama): String {
    val year = drama.createdAt.take(4).ifBlank { "2026" }
    val serial = drama.updatedAt.filter(Char::isDigit).takeLast(6).ifBlank { "597301" }
    return "(番茄) 网微剧备案字 ($year) 第${serial}号"
}

internal const val HOME_MENU_ENTRY_CONTENT_DESCRIPTION = "打开菜单"
internal const val HOME_SEARCH_ENTRY_CONTENT_DESCRIPTION = "打开搜索"

private val HOME_FRAME_CTA_HEIGHT = 60.dp
private val HOME_FRAME_CTA_VERTICAL_MARGIN = 16.dp
private val HOME_FEED_BOTTOM_CONTENT_SPACING = 12.dp

internal fun shouldRenderCheckInPopup(isPopupVisible: Boolean, hasBlockingModal: Boolean): Boolean {
    return isPopupVisible && !hasBlockingModal
}

internal fun hasNavigableDramaId(dramaId: String): Boolean = dramaId.trim().isNotEmpty()

internal fun homeFeedBottomContentPadding(
    ctaHeight: Dp = HOME_FRAME_CTA_HEIGHT,
    ctaVerticalMargin: Dp = HOME_FRAME_CTA_VERTICAL_MARGIN,
    extraSpacing: Dp = HOME_FEED_BOTTOM_CONTENT_SPACING,
): Dp {
    return ctaHeight + ctaVerticalMargin + extraSpacing
}

internal fun currentHomeFeedDrama(items: List<Drama>, currentPage: Int): Drama? {
    if (items.isEmpty()) {
        return null
    }
    return items.getOrNull(currentPage.coerceIn(items.indices))
}

internal fun activePlayerStateForDrama(
    dramaId: String,
    currentPage: Int,
    page: Int,
    activeDramaId: String?,
    activePlayerUiState: PlayerUiState,
): PlayerUiState? {
    return if (page == currentPage && dramaId == activeDramaId) {
        activePlayerUiState
    } else {
        null
    }
}

internal fun homeFeedPlayerStatusCopy(uiState: PlayerUiState): String? {
    return when (uiState.screenState) {
        PlayerScreenState.IDLE,
        PlayerScreenState.READY,
        PlayerScreenState.PLAYING,
        -> null

        PlayerScreenState.BOOTSTRAPPING,
        PlayerScreenState.SWITCHING_EPISODE,
        -> "正在准备视频..."

        PlayerScreenState.PAUSED -> "视频已暂停"
        PlayerScreenState.NO_RESOURCE -> uiState.errorMessage.orEmpty().ifBlank { "暂无可播放内容" }
        PlayerScreenState.ERROR -> uiState.errorMessage.orEmpty().ifBlank { "视频加载失败，请重试" }
    }
}

internal fun buildDramaMeta(drama: Drama): String {
    val parts = buildList {
        if (drama.category.isNotBlank()) {
            add(drama.category)
        }
        if (drama.tags.isNotEmpty()) {
            add(drama.tags.take(3).joinToString(" · "))
        }
        if (drama.episodeCount > 0) {
            add("第1集 | 共${drama.episodeCount}集")
        }
        if (drama.rating > 0.0) {
            add("评分 ${String.format("%.1f", drama.rating)}")
        }
    }
    return parts.joinToString("  ")
}

internal fun buildFrameCtaTitle(episodeCount: Int): String {
    return "观看完整漫剧 · 全${episodeCount.coerceAtLeast(1)}集"
}

internal fun formatCompactCount(value: Int): String {
    return if (value >= 10_000) {
        val wan = value / 10_000.0
        val text = if (wan >= 10) {
            String.format("%.1f", wan)
        } else {
            String.format("%.2f", wan)
        }.trimEnd('0').trimEnd('.')
        "${text}万"
    } else {
        value.toString()
    }
}
