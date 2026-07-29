package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentUiModel
import com.djs66256.short_drama.feature.comments.viewmodel.CommentListState
import com.djs66256.short_drama.feature.comments.viewmodel.CommentUiState

@Composable
fun CommentHeader(
    totalCount: Int,
    selectedSort: CommentSort,
    onSelectSort: (CommentSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "评论 · $totalCount",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommentSort.entries.forEach { sort ->
                FilterChip(
                    selected = sort == selectedSort,
                    onClick = { onSelectSort(sort) },
                    label = { Text(sort.label) },
                )
            }
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (hasNextPage) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(onClick = onLoadMore) {
                        Text("加载更多")
                    }
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = comment.userDisplayName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = comment.createdAt,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(
                onClick = onToggleLike,
                enabled = !liking,
                label = {
                    Text(if (comment.liked) "已赞 ${comment.likeCount}" else "点赞 ${comment.likeCount}")
                },
            )
        }
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("写下你的评论") },
            supportingText = {
                val supportText = errorMessage ?: "${inputText.trim().length}/500"
                Text(supportText)
            },
            isError = errorMessage != null,
            minLines = 2,
            maxLines = 4,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
            ) {
                Text(if (isSubmitting) "发送中" else "发送")
            }
        }
    }
}

@Composable
fun CommentLoadingState(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun CommentEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("暂无评论", style = MaterialTheme.typography.titleMedium)
        Text("快来抢沙发", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun CommentErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("加载失败", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onLoadMore) {
            Text("重试加载更多")
        }
    }
}
