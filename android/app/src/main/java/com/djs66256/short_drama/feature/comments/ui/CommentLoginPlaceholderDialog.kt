package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.djs66256.short_drama.feature.comments.model.CommentLoginContext
import com.djs66256.short_drama.feature.comments.model.CommentSource

@Composable
fun CommentLoginPlaceholderDialog(
    context: CommentLoginContext,
    onConfirmLogin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text("请先登录")
        },
        text = {
            Text(commentLoginPlaceholderMessage(context))
        },
        confirmButton = {
            TextButton(onClick = onConfirmLogin) {
                Text("模拟登录完成")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        },
    )
}

internal fun commentLoginPlaceholderMessage(context: CommentLoginContext): String {
    return buildString {
        append("当前登录能力仍是占位流程。确认后会返回")
        append(context.source.label)
        append("并重新打开评论抽屉，但不会自动重试刚才的操作。")
    }
}

private val CommentSource.label: String
    get() = when (this) {
        CommentSource.HOME -> "首页"
        CommentSource.PLAYER -> "播放器"
    }
