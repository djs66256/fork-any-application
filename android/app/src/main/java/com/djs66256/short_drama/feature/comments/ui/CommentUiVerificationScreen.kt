package com.djs66256.short_drama.feature.comments.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val VerificationSheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
private val VerificationStatusBarText = Color(0xFFF8F8F8)
private val VerificationStatusBarIcon = Color(0xFFF8F8F8)
private val VerificationHomeIndicatorColor = Color(0xFF1D1D1D)
private val VerificationStatusBarInsetTop = 14.dp
private val VerificationStatusBarInsetHorizontal = 28.dp

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
                    .offset(y = 220.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color(0x1A000000),
                                Color(0x55000000),
                            ),
                        ),
                    ),
            )
            VerificationStatusBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = VerificationStatusBarInsetTop)
                    .padding(horizontal = VerificationStatusBarInsetHorizontal),
            )
            VerificationHomeIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .shadow(elevation = 14.dp, shape = VerificationSheetShape)
                    .offset(y = 10.dp),
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
                        .height(414.dp),
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
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "11:57",
                color = VerificationStatusBarText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Medium,
            )
            VerificationShieldIcon()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VerificationSignalBars()
            VerificationWifiIcon()
            VerificationBatteryIcon()
        }
    }
}

@Composable
private fun VerificationShieldIcon() {
    Canvas(modifier = Modifier.size(width = 16.dp, height = 20.dp)) {
        val path = Path().apply {
            moveTo(size.width * 0.5f, 0f)
            lineTo(size.width * 0.9f, size.height * 0.14f)
            lineTo(size.width * 0.82f, size.height * 0.72f)
            quadraticTo(
                size.width * 0.76f,
                size.height * 0.92f,
                size.width * 0.5f,
                size.height,
            )
            quadraticTo(
                size.width * 0.24f,
                size.height * 0.92f,
                size.width * 0.18f,
                size.height * 0.72f,
            )
            lineTo(size.width * 0.1f, size.height * 0.14f)
            close()
        }
        drawPath(path = path, color = VerificationStatusBarIcon, style = Stroke(width = size.width * 0.12f))
    }
}

@Composable
private fun VerificationSignalBars() {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        listOf(10.dp, 14.dp, 18.dp, 22.dp).forEach { barHeight ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(100.dp))
                    .background(VerificationStatusBarIcon),
            )
        }
    }
}

@Composable
private fun VerificationWifiIcon() {
    Canvas(modifier = Modifier.size(width = 24.dp, height = 18.dp)) {
        val stroke = Stroke(width = 2.8f, cap = StrokeCap.Round)
        drawArc(
            color = VerificationStatusBarIcon,
            startAngle = 210f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(0f, size.height * 0.1f),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 1.15f),
            style = stroke,
        )
        drawArc(
            color = VerificationStatusBarIcon,
            startAngle = 218f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = Offset(size.width * 0.14f, size.height * 0.3f),
            size = androidx.compose.ui.geometry.Size(size.width * 0.72f, size.height * 0.72f),
            style = stroke,
        )
        drawCircle(
            color = VerificationStatusBarIcon,
            radius = 2.3f,
            center = Offset(size.width / 2f, size.height * 0.82f),
        )
    }
}

@Composable
private fun VerificationBatteryIcon() {
    Canvas(modifier = Modifier.size(width = 40.dp, height = 18.dp)) {
        val outline = RoundRect(
            rect = Rect(0f, 1f, size.width - 5f, size.height - 1f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f),
        )
        drawRoundRect(
            color = VerificationStatusBarIcon,
            topLeft = Offset(outline.left, outline.top),
            size = androidx.compose.ui.geometry.Size(outline.width, outline.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f),
            style = Stroke(width = 2.6f),
        )
        drawRoundRect(
            color = VerificationStatusBarIcon,
            topLeft = Offset(4f, 5f),
            size = androidx.compose.ui.geometry.Size(size.width - 13f, size.height - 10f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f),
        )
        drawRoundRect(
            color = VerificationStatusBarIcon,
            topLeft = Offset(size.width - 4f, size.height * 0.32f),
            size = androidx.compose.ui.geometry.Size(4f, size.height * 0.34f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
        )
    }
}

@Composable
private fun VerificationHomeIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(156.dp)
            .height(5.dp)
            .background(VerificationHomeIndicatorColor, RoundedCornerShape(100.dp)),
    )
}
