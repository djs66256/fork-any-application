package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentUiModel
import com.djs66256.short_drama.feature.comments.viewmodel.CommentListState
import com.djs66256.short_drama.feature.comments.viewmodel.CommentUiState

const val COMMENT_UI_VERIFICATION_SCREEN = "comment_ui_verification"

@Composable
fun CommentUiVerificationScreen(
    modifier: Modifier = Modifier,
) {
    val uiState = CommentUiState(
        dramaId = "comment-ui-verification",
        listState = CommentListState.Content,
        selectedSort = CommentSort.LATEST,
        comments = verificationComments(),
        totalCount = 470,
        inputText = "",
        hasNextPage = true,
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        CommentBottomSheetContent(
            uiState = uiState,
            onSelectSort = {},
            onRetry = {},
            onToggleLike = {},
            onLoadMore = {},
            onInputChanged = {},
            onSubmit = {},
            modifier = Modifier.fillMaxWidth(),
            onDismiss = {},
        )
    }
}

private fun verificationComments(): List<CommentUiModel> {
    return listOf(
        CommentUiModel(
            id = "comment-1",
            userDisplayName = "用户名31750495",
            userAvatarUrl = null,
            content = "敢情这些蛋特么都是捡来的，不是亲生的😂",
            likeCount = 2839,
            liked = false,
            createdAt = "07-20",
        ),
        CommentUiModel(
            id = "comment-2",
            userDisplayName = "用户名50499622",
            userAvatarUrl = null,
            content = "大荒龙君",
            likeCount = 2,
            liked = false,
            createdAt = "2天前",
        ),
        CommentUiModel(
            id = "comment-3",
            userDisplayName = "用户名69744097",
            userAvatarUrl = null,
            content = "红果，你咋知道我抖音刚刷到要来找这个???",
            likeCount = 4,
            liked = false,
            createdAt = "2天前",
        ),
        CommentUiModel(
            id = "comment-4",
            userDisplayName = "小铮哥",
            userAvatarUrl = null,
            content = "兄弟你好香",
            likeCount = 1,
            liked = false,
            createdAt = "3天前",
        ),
        CommentUiModel(
            id = "comment-5",
            userDisplayName = "女儿阁的流架",
            userAvatarUrl = null,
            content = "",
            likeCount = 0,
            liked = false,
            createdAt = "刚刚",
        ),
        CommentUiModel(
            id = "comment-6",
            userDisplayName = "匿名用户",
            userAvatarUrl = null,
            content = "",
            likeCount = 0,
            liked = false,
            createdAt = "刚刚",
        ),
    )
}
