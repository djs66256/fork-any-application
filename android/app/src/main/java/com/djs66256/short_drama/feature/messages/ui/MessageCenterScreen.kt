package com.djs66256.short_drama.feature.messages.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djs66256.short_drama.domain.model.InteractionMessage
import com.djs66256.short_drama.domain.model.SystemMessage
import com.djs66256.short_drama.feature.messages.viewmodel.MessageCenterViewModel

@Composable
fun MessageCenterScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MessageCenterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val systemErrorMessage = uiState.systemErrorMessage
    val interactionErrorMessage = uiState.interactionErrorMessage

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
                Text(
                    text = "我的消息",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MessageSectionCard(title = "系统消息") {
                    when {
                        uiState.isSystemLoading -> LoadingBlock("正在加载系统消息...")
                        systemErrorMessage != null -> ErrorBlock(
                            message = systemErrorMessage,
                            onRetry = viewModel::retrySystemMessages,
                        )
                        uiState.systemMessages.isEmpty() -> EmptyBlock("暂无系统消息")
                        else -> SystemMessageList(items = uiState.systemMessages)
                    }
                }
            }

            item {
                MessageSectionCard(title = "互动消息") {
                    when {
                        uiState.showInteractionLoginGate -> InteractionLoginGate(onLogin = onLogin)
                        uiState.isInteractionLoading -> LoadingBlock("正在加载互动消息...")
                        interactionErrorMessage != null -> ErrorBlock(
                            message = interactionErrorMessage,
                            onRetry = viewModel::retryInteractionMessages,
                        )
                        uiState.interactionMessages.isEmpty() -> EmptyBlock("暂无互动消息")
                        else -> InteractionMessageList(items = uiState.interactionMessages)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun LoadingBlock(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorBlock(
    message: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        OutlinedButton(onClick = onRetry) {
            Text("重试")
        }
    }
}

@Composable
private fun EmptyBlock(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun InteractionLoginGate(onLogin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "登录后可查看评论回复、点赞提醒等互动消息。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onLogin) {
            Text("登录查看互动消息")
        }
    }
}

@Composable
private fun SystemMessageList(items: List<SystemMessage>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            MessageListItem(
                title = item.title,
                summary = item.summary,
                time = item.sentAt,
            )
            if (index != items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InteractionMessageList(items: List<InteractionMessage>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, item ->
            MessageListItem(
                title = item.title,
                summary = item.summary,
                time = item.sentAt,
            )
            if (index != items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MessageListItem(
    title: String,
    summary: String,
    time: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
