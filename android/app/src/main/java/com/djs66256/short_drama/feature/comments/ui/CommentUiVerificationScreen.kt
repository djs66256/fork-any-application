package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djs66256.short_drama.domain.model.CommentSort
import com.djs66256.short_drama.feature.comments.model.CommentUiModel
import com.djs66256.short_drama.feature.comments.viewmodel.CommentListState
import com.djs66256.short_drama.feature.comments.viewmodel.CommentUiState
import com.djs66256.short_drama.feature.player.player.PlaceholderPlayerHost
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState

const val COMMENT_UI_VERIFICATION_SCREEN = "comment_ui_verification"

private val VerificationSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val VerificationStatusBarText = Color(0xFFF8F8F8)
private val VerificationStatusBarIcon = Color(0xFFF8F8F8)
private val VerificationDeviceChrome = Color(0xFFEFEFEF)

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
        hasNextPage = false,
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PlaceholderPlayerHost(
                uiState = PlayerUiState(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .offset(y = 176.dp),
            )
            VerificationStatusBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
                    .padding(horizontal = 32.dp),
            )
            VerificationHomeIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color(0x26000000),
                                Color(0x66000000),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(elevation = 18.dp, shape = VerificationSheetShape),
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
                    listModifier = Modifier
                        .fillMaxWidth()
                        .height(408.dp),
                    onDismiss = {},
                )
            }
        }
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
    )
}

@Composable
private fun VerificationStatusBar(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "11:57",
            color = VerificationStatusBarText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(VerificationStatusBarIcon, RoundedCornerShape(7.dp)),
            )
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .height(16.dp)
                    .background(VerificationStatusBarIcon, RoundedCornerShape(8.dp)),
            )
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(20.dp)
                    .background(Color.Transparent, RoundedCornerShape(10.dp))
                    .background(VerificationStatusBarIcon, RoundedCornerShape(10.dp)),
            )
        }
    }
}

@Composable
private fun VerificationHomeIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(154.dp)
            .height(6.dp)
            .background(VerificationDeviceChrome, RoundedCornerShape(100.dp)),
    )
}
