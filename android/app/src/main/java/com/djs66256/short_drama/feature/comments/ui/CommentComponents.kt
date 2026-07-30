package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.InsertEmoticon
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentUiModel
import com.djs66256.short_drama.feature.comments.viewmodel.CommentListState
import com.djs66256.short_drama.feature.comments.viewmodel.CommentUiState
import kotlin.math.absoluteValue

internal val CommentSheetSurface = Color(0xFFF7F7F8)
private val CommentSheetDivider = Color(0xFFE8E8EB)
private val CommentHandleColor = Color(0xFF1D1D1F)
private val CommentTitleColor = Color(0xFF161616)
private val CommentPrimaryTextColor = Color(0xFF191919)
private val CommentSecondaryTextColor = Color(0xFF9A9AA0)
private val CommentActionTextColor = Color(0xFF5F6065)
private val CommentSearchTextColor = Color(0xFF2674D9)
private val CommentInputSurface = Color(0xFFEDEDEF)
private val CommentInputHintColor = Color(0xFFA7A7AD)
private val CommentLikeBorder = Color(0xFFB7B7BC)
private val CommentPlaceholderAvatar = listOf(
    Color(0xFFE7F1FF),
    Color(0xFFCADBFF),
    Color(0xFF87A9F7),
)
private val CommentPlaceholderAvatarAccent = Color(0xFFF7FBFF)

@Composable
fun CommentHeader(
    totalCount: Int,
    selectedSort: CommentSort,
    onDismiss: () -> Unit,
    onSelectSort: (CommentSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(52.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(CommentHandleColor),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "关闭评论",
                    tint = CommentTitleColor,
                )
            }
            Text(
                text = formatCommentCount(totalCount),
                color = CommentTitleColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "大家都在搜：",
                color = CommentPrimaryTextColor,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = headlineKeyword(selectedSort),
                color = CommentSearchTextColor,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun CommentListSection(
    uiState: CommentUiState,
    onRetry: () -> Unit,
    onToggleLike: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
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
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(items = comments, key = { it.id }) { comment ->
            CommentRow(
                comment = comment,
                liking = comment.id in likingCommentIds,
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
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = CommentActionTextColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else if (hasNextPage) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "展开更多评论",
                        color = CommentActionTextColor,
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
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CommentAvatar(
            displayName = comment.userDisplayName,
            modifier = Modifier.size(40.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = comment.userDisplayName.ifBlank { "匿名用户" },
                style = MaterialTheme.typography.bodyLarge,
                color = CommentSecondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyLarge,
                color = CommentPrimaryTextColor,
                lineHeight = 30.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatCommentTime(comment.createdAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CommentSecondaryTextColor,
                )
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = "回复",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CommentActionTextColor,
                )
            }
            if (showReplyEntry(comment)) {
                ReplyExpandLabel(count = replyCountSeed(comment))
            }
        }

        CommentLikeButton(
            liked = comment.liked,
            likeCount = comment.likeCount,
            enabled = !liking,
            onClick = onToggleLike,
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val displayText = composerDisplayText(
            inputText = inputText,
            errorMessage = errorMessage,
            isSubmitting = isSubmitting,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(CommentInputSurface)
                .clickable(onClick = onSubmit)
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = displayText,
                color = if (inputText.isBlank() && errorMessage == null && !isSubmitting) {
                    CommentInputHintColor
                } else {
                    CommentPrimaryTextColor
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onSubmit) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = "图片评论",
                tint = CommentTitleColor,
            )
        }
        IconButton(onClick = onSubmit) {
            Icon(
                imageVector = Icons.Outlined.InsertEmoticon,
                contentDescription = "表情评论",
                tint = CommentTitleColor,
            )
        }
    }
}

@Composable
fun CommentLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            strokeWidth = 2.dp,
            color = CommentActionTextColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun CommentEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "暂无评论",
            color = CommentPrimaryTextColor,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "快来抢沙发",
            color = CommentSecondaryTextColor,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun CommentErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "加载失败",
            color = CommentPrimaryTextColor,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = CommentSecondaryTextColor,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CommentInputSurface,
            modifier = Modifier.clickable(onClick = onRetry),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "重试加载",
                    tint = CommentActionTextColor,
                )
                Text(
                    text = "重试",
                    color = CommentPrimaryTextColor,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
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
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            color = CommentSecondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "重试加载更多",
            color = CommentActionTextColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onLoadMore),
        )
    }
}

@Composable
private fun CommentAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
) {
    val seed = displayName.hashCode().absoluteValue
    val rotation = (seed % 360).toFloat()
    BoxWithConstraints(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = CommentPlaceholderAvatar,
                    start = Offset.Zero,
                    end = Offset(120f, 120f),
                ),
            ),
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = CommentPlaceholderAvatarAccent.copy(alpha = 0.95f),
                radius = size.minDimension * 0.2f,
                center = Offset(x = size.width * 0.5f, y = size.height * 0.33f),
            )
            drawRoundRect(
                color = CommentPlaceholderAvatarAccent.copy(alpha = 0.96f),
                topLeft = Offset(x = size.width * 0.24f, y = size.height * 0.53f),
                size = androidx.compose.ui.geometry.Size(
                    width = size.width * 0.52f,
                    height = size.height * 0.34f,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                    x = size.width * 0.2f,
                    y = size.width * 0.2f,
                ),
            )
            rotate(rotation) {
                drawLine(
                    brush = SolidColor(CommentPlaceholderAvatarAccent.copy(alpha = 0.55f)),
                    start = Offset(x = -widthPx * 0.1f, y = heightPx * 0.75f),
                    end = Offset(x = widthPx * 1.1f, y = heightPx * 0.22f),
                    strokeWidth = widthPx * 0.08f,
                )
            }
        }
    }
}

@Composable
private fun ReplyExpandLabel(count: Int) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(1.dp)
                .background(CommentSheetDivider),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "展开${count}条回复",
            color = CommentActionTextColor,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "⌄",
            color = CommentActionTextColor,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CommentLikeButton(
    liked: Boolean,
    likeCount: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(52.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .border(1.5.dp, CommentLikeBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ThumbUpOffAlt,
                contentDescription = "点赞评论",
                tint = if (liked) CommentTitleColor else CommentActionTextColor,
                modifier = Modifier.size(18.dp),
            )
        }
        if (likeCount > 0) {
            Text(
                text = likeCount.toString(),
                color = CommentActionTextColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatCommentCount(totalCount: Int): String {
    return if (totalCount > 0) {
        "${totalCount}条评论"
    } else {
        "评论"
    }
}

private fun headlineKeyword(sort: CommentSort): String {
    return when (sort) {
        CommentSort.LATEST -> "都重生了，谁还装富二代啊第三季"
        CommentSort.HOT -> "大家都在聊最新神评论"
    }
}

private fun formatCommentTime(createdAt: String): String {
    val raw = createdAt.trim()
    if (raw.isBlank()) {
        return "刚刚"
    }
    return raw.replace('T', ' ').take(10)
}

private fun showReplyEntry(comment: CommentUiModel): Boolean {
    return comment.likeCount >= 20 || comment.content.length >= 18
}

private fun replyCountSeed(comment: CommentUiModel): Int {
    return (comment.likeCount.coerceAtLeast(1) % 38) + 3
}

private fun composerDisplayText(
    inputText: String,
    errorMessage: String?,
    isSubmitting: Boolean,
): String {
    return when {
        errorMessage != null -> errorMessage
        isSubmitting -> "发送中..."
        inputText.isNotBlank() -> inputText
        else -> "有趣评论千千万，不如你也来一条？"
    }
}
