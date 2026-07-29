package com.djs66256.short_drama.feature.home.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.feature.comments.model.CommentLoginContext
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.ui.CommentBottomSheet
import com.djs66256.short_drama.feature.comments.ui.CommentLoginPlaceholderDialog
import com.djs66256.short_drama.feature.home.viewmodel.HomeViewModel

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
    var activeCommentDramaId by remember { mutableStateOf<String?>(null) }
    var pendingCommentLoginContext by remember { mutableStateOf<CommentLoginContext?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    val errorMessage = uiState.errorMessage

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HomeTopBar(
                onOpenMenu = onOpenMenu,
                onOpenSearch = onOpenSearch,
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> HomeFeedLoadingState(isRetrying = uiState.isRetrying)
                    errorMessage != null -> HomeFeedErrorState(
                        message = errorMessage,
                        onRetry = viewModel::retry,
                    )
                    uiState.items.isEmpty() -> HomeFeedEmptyState()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(items = uiState.items, key = { it.id }) { drama ->
                            HomeDramaCard(
                                drama = drama,
                                onPlay = { onOpenPlay(drama.id) },
                                onDetail = { onOpenDetail(drama.id) },
                                onComment = { activeCommentDramaId = drama.id },
                            )
                        }
                    }
                }
            }
        }

        val hasBlockingModal = activeCommentDramaId != null || pendingCommentLoginContext != null
        if (hasBlockingModal) {
            LaunchedEffect(hasBlockingModal) {
                viewModel.abandonCheckInPopupForCurrentSession()
            }
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
private fun HomeTopBar(
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = HOME_MENU_ENTRY_CONTENT_DESCRIPTION,
            )
        }
        Text(
            text = "首页",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onOpenSearch) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = HOME_SEARCH_ENTRY_CONTENT_DESCRIPTION,
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
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRetrying) "正在重新加载首页内容..." else "正在加载首页内容...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前还没有可展示的短剧，稍后再来看看。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
fun HomeDramaCard(
    drama: Drama,
    onPlay: () -> Unit,
    onDetail: () -> Unit,
    onComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionsEnabled = hasNavigableDramaId(drama.id)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DramaCoverPlaceholder(drama = drama)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = drama.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = drama.description.ifBlank { "暂无简介" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                val metaText = buildDramaMeta(drama)
                if (metaText.isNotBlank()) {
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        if (actionsEnabled) {
                            onPlay()
                        }
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("观看")
                }
                OutlinedButton(
                    onClick = {
                        if (actionsEnabled) {
                            onDetail()
                        }
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("详情")
                }
                OutlinedButton(
                    onClick = {
                        if (actionsEnabled) {
                            onComment()
                        }
                    },
                    enabled = actionsEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("评论")
                }
            }
        }
    }
}

@Composable
private fun DramaCoverPlaceholder(drama: Drama) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = if (drama.coverUrl.isBlank()) "暂无封面" else "封面已配置",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (drama.category.isNotBlank()) {
                Text(
                    text = drama.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

internal const val HOME_MENU_ENTRY_CONTENT_DESCRIPTION = "打开菜单"
internal const val HOME_SEARCH_ENTRY_CONTENT_DESCRIPTION = "打开搜索"

internal fun shouldRenderCheckInPopup(isPopupVisible: Boolean, hasBlockingModal: Boolean): Boolean {
    return isPopupVisible && !hasBlockingModal
}

internal fun hasNavigableDramaId(dramaId: String): Boolean = dramaId.trim().isNotEmpty()

internal fun buildDramaMeta(drama: Drama): String {
    val parts = buildList {
        if (drama.category.isNotBlank()) {
            add(drama.category)
        }
        if (drama.tags.isNotEmpty()) {
            add(drama.tags.take(2).joinToString(" / "))
        }
        if (drama.episodeCount > 0) {
            add("${drama.episodeCount} 集")
        }
        if (drama.rating > 0.0) {
            add("评分 ${String.format("%.1f", drama.rating)}")
        }
    }
    return parts.joinToString(" · ")
}
