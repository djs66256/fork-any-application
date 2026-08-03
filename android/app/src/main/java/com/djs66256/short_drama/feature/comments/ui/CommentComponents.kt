package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertEmoticon
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentUiModel
import com.djs66256.short_drama.feature.comments.viewmodel.CommentListState
import com.djs66256.short_drama.feature.comments.viewmodel.CommentUiState

private val CommentSheetBackground = Color(0xFFF8F8F8)
private val CommentDividerColor = Color(0xFFE7E7E7)
private val CommentAvatarPalette = listOf(
    Color(0xFFF7D0D9),
    Color(0xFFD7E6FF),
    Color(0xFFFFE2BF),
    Color(0xFFD8F0E2),
    Color(0xFFE8D9FF),
)
private val CommentPrimaryText = Color(0xFF111111)
private val CommentSecondaryText = Color(0xFF999999)
private val CommentLinkBlue = Color(0xFF2E6FE8)
private val CommentIconTint = Color(0xFF151515)
private val CommentLikedTint = Color(0xFFEF4444)
private val CommentCountText = Color(0xFF1A1A1A)
private val CommentSheetTopRadius = 28.dp
private val CommentAvatarSize = 42.dp
private val CommentBottomInputHeight = 48.dp

@Composable
fun CommentHeader(
    totalCount: Int,
    selectedSort: CommentSort,
    onSelectSort: (CommentSort) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            onDismiss?.let {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "收起评论",
                    tint = CommentPrimaryText,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(28.dp)
                        .clickable(onClick = it),
                )
            }
            Text(
                text = "${totalCount}条评论",
                style = MaterialTheme.typography.titleLarge,
                color = CommentPrimaryText,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        CommentSearchRow(
            onSelectSort = onSelectSort,
            selectedSort = selectedSort,
        )
    }
}

@Composable
private fun CommentSearchRow(
    selectedSort: CommentSort,
    onSelectSort: (CommentSort) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "大家都在搜：",
            style = MaterialTheme.typography.bodyLarge,
            color = CommentPrimaryText,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "都重生了，谁还装富二代啊第三季",
            style = MaterialTheme.typography.bodyLarge,
            color = CommentLinkBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
fun CommentListSection(
    uiState: CommentUiState,
    onRetry: () -> Unit,
    onToggleLike: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 8.dp),
) {
    when (uiState.listState) {
        CommentListState.Loading -> CommentLoadingState(modifier = modifier)
        CommentListState.Error -> CommentErrorState(
            message = uiState.errorMessage.orEmpty().ifBlank { "加载失败，请重试" },
            onRetry = onRetry,
            modifier = modifier,
        )
        CommentListState.Empty -> CommentEmptyState(modifier = modifier)
        CommentListState.Content -> CommentItems(
            comments = uiState.comments,
            likingCommentIds = uiState.likingCommentIds,
            appendErrorMessage = uiState.appendErrorMessage,
            isAppending = uiState.isAppending,
            hasNextPage = uiState.hasNextPage,
            onToggleLike = onToggleLike,
            onLoadMore = onLoadMore,
            modifier = modifier,
            contentPadding = contentPadding,
        )
        CommentListState.Idle -> CommentLoadingState(modifier = modifier)
    }
}

@Composable
fun CommentItems(
    comments: List<CommentUiModel>,
    likingCommentIds: Set<String>,
    appendErrorMessage: String?,
    isAppending: Boolean,
    hasNextPage: Boolean,
    onToggleLike: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 8.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = contentPadding,
    ) {
        itemsIndexed(items = comments, key = { _, item -> item.id }) { index, comment ->
            CommentRow(
                comment = comment,
                liking = comment.id in likingCommentIds,
                showExpandReplies = index == 0,
                onToggleLike = { onToggleLike(comment.id) },
            )
        }
        if (appendErrorMessage != null) {
            item {
                CommentAppendFooter(message = appendErrorMessage, onLoadMore = onLoadMore)
            }
        } else if (isAppending) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                }
            }
        } else if (hasNextPage) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "加载更多",
                        color = CommentSecondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable(onClick = onLoadMore),
                    )
                }
            }
        }
    }
}

@Composable
fun CommentRow(
    comment: CommentUiModel,
    liking: Boolean,
    showExpandReplies: Boolean,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CommentAvatar(
            name = comment.userDisplayName,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = comment.userDisplayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = CommentSecondaryText,
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyLarge,
                color = CommentPrimaryText,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = comment.createdAt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CommentSecondaryText,
                )
                Text(
                    text = "回复",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CommentSecondaryText,
                )
            }
            if (showExpandReplies) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(1.dp)
                            .background(CommentDividerColor),
                    )
                    Text(
                        text = "展开35条回复",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF5D5D5D),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF5D5D5D),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        CommentLikeButton(
            likeCount = comment.likeCount,
            liked = comment.liked,
            enabled = !liking,
            onClick = onToggleLike,
        )
    }
}

@Composable
private fun CommentAvatar(
    name: String,
    modifier: Modifier = Modifier,
) {
    val paletteColor = remember(name) {
        val safeHash = name.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
        CommentAvatarPalette[safeHash % CommentAvatarPalette.size]
    }
    Box(
        modifier = modifier
            .size(CommentAvatarSize)
            .clip(CircleShape)
            .background(paletteColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).ifBlank { "评" },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CommentLikeButton(
    likeCount: Int,
    liked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = "点赞",
            tint = if (liked) CommentLikedTint else CommentSecondaryText,
            modifier = Modifier.size(30.dp),
        )
        Text(
            text = likeCount.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = CommentCountText,
        )
    }
}

@Composable
fun CommentComposer(
    inputText: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(CommentBottomInputHeight),
                placeholder = {
                    Text(
                        "有趣评论千千万，不如你也来一条？",
                        color = CommentSecondaryText,
                    )
                },
                singleLine = true,
                maxLines = 1,
                shape = RoundedCornerShape(24.dp),
                enabled = !isSubmitting,
            )
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = "图片",
                tint = CommentIconTint,
                modifier = Modifier.size(32.dp),
            )
            Icon(
                imageVector = Icons.Outlined.InsertEmoticon,
                contentDescription = "表情",
                tint = CommentIconTint,
                modifier = Modifier.size(32.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = if (isSubmitting) "发送中" else "发送",
                style = MaterialTheme.typography.labelLarge,
                color = if (isSubmitting) CommentSecondaryText else CommentLinkBlue,
                modifier = Modifier.clickable(enabled = !isSubmitting, onClick = onSubmit),
            )
        }
    }
}

@Composable
fun CommentLoadingState(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun CommentEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("暂无评论", style = MaterialTheme.typography.titleMedium, color = CommentPrimaryText)
        Text("快来抢沙发", style = MaterialTheme.typography.bodyMedium, color = CommentSecondaryText)
    }
}

@Composable
fun CommentErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("加载失败", style = MaterialTheme.typography.titleMedium, color = CommentPrimaryText)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = CommentSecondaryText)
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun CommentAppendFooter(
    message: String,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = CommentSecondaryText)
        Text(
            text = "重试加载更多",
            style = MaterialTheme.typography.bodyMedium,
            color = CommentLinkBlue,
            modifier = Modifier.clickable(onClick = onLoadMore),
        )
    }
}

@Composable
fun PlayerCommentPreview(
    commentText: String,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenComments),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "热评：",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = commentText,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = "展开评论",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun CommentSheetContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CommentSheetBackground,
        shape = RoundedCornerShape(topStart = CommentSheetTopRadius, topEnd = CommentSheetTopRadius),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CommentSheetBackground)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            content = content,
        )
    }
}

@Composable
fun CommentSheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFD5D5D5)),
        )
    }
}
