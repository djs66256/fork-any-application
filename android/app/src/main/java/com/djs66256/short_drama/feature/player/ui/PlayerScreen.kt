package com.djs66256.short_drama.feature.player.ui

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.feature.comments.model.CommentLoginContext
import com.djs66256.short_drama.feature.comments.ui.CommentBottomSheet
import com.djs66256.short_drama.feature.comments.ui.CommentLoginPlaceholderDialog
import com.djs66256.short_drama.feature.player.player.PlaceholderPlayerHost
import com.djs66256.short_drama.feature.player.ui.components.EpisodePickerSheetContent
import com.djs66256.short_drama.feature.player.ui.components.PlayerBottomInfo
import com.djs66256.short_drama.feature.player.ui.components.PlayerCenterBadge
import com.djs66256.short_drama.feature.player.ui.components.PlayerEpisodeDock
import com.djs66256.short_drama.feature.player.ui.components.PlayerProgressBar
import com.djs66256.short_drama.feature.player.ui.components.PlayerRightActionBar
import com.djs66256.short_drama.feature.player.ui.components.PlayerStatusContent
import com.djs66256.short_drama.feature.player.ui.components.PlayerTopBar
import com.djs66256.short_drama.feature.player.ui.components.SpeedPickerSheetContent
import com.djs66256.short_drama.feature.player.viewmodel.PlaybackSpeed
import com.djs66256.short_drama.feature.player.viewmodel.PlayerEffect
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerViewModel

private val PlayerPageBackground = Color(0xFF000000)
private val PlayerStatusDialogBackground = Color(0xCC1A1A1A)

internal enum class PlayerContentVariant {
    LOADING,
    ERROR,
    NO_RESOURCE,
    CONTENT,
}

private data class PlayerContentCallbacks(
    val onBack: () -> Unit,
    val onToggleSpeedSheet: () -> Unit,
    val onToggleMoreSheet: () -> Unit,
    val onToggleEpisodeSheet: () -> Unit,
    val onToggleLike: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onOpenComments: () -> Unit,
    val onSelectSpeed: (PlaybackSpeed) -> Unit,
    val onSelectEpisode: (Episode) -> Unit,
    val onCommentLoginRequired: (CommentLoginContext) -> Unit,
    val onCommentMessage: (String) -> Unit,
    val onDismissComments: () -> Unit,
    val onPlaybackCompleted: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController? = null,
    onBack: (() -> Unit)? = null,
    onPlaybackCompleted: (() -> Unit)? = null,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val fallbackNavController = rememberNavController()
    val activeNavController = navController ?: fallbackNavController
    val resolvedOnBack: () -> Unit = onBack ?: { activeNavController.popBackStack(); Unit }
    var pendingCommentLoginContext by remember { mutableStateOf<CommentLoginContext?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadIfNeeded()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenDisposed()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PlayerEffect.RequireLogin -> {
                    viewModel.closeComments()
                    pendingCommentLoginContext = effect.context
                    Toast.makeText(context, "请先登录后再操作评论", Toast.LENGTH_SHORT).show()
                }
                is PlayerEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SideEffect {
        (context as? Activity)?.window?.let { window ->
            window.statusBarColor = AndroidColor.BLACK
            window.navigationBarColor = AndroidColor.BLACK
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    when (playerContentVariant(uiState)) {
        PlayerContentVariant.LOADING -> PlayerStatusScene(
            title = "正在进入播放器",
            message = "按 progress → episodes → start 顺序初始化中...",
            modifier = Modifier.fillMaxSize(),
        )

        PlayerContentVariant.ERROR -> PlayerStatusScene(
            title = "加载失败",
            message = uiState.errorMessage.orEmpty().ifBlank { "加载失败，请重试" },
            actionLabel = "重试",
            onAction = viewModel::retry,
            modifier = Modifier.fillMaxSize(),
        )

        PlayerContentVariant.NO_RESOURCE -> PlayerStatusScene(
            title = "暂无可播放内容",
            message = uiState.errorMessage.orEmpty().ifBlank { "当前短剧暂无可播放剧集" },
            actionLabel = "重试",
            onAction = viewModel::retry,
            modifier = Modifier.fillMaxSize(),
        )

        PlayerContentVariant.CONTENT -> PlayerContent(
            uiState = uiState,
            callbacks = PlayerContentCallbacks(
                onBack = resolvedOnBack,
                onToggleSpeedSheet = viewModel::toggleSpeedSheet,
                onToggleMoreSheet = {},
                onToggleEpisodeSheet = viewModel::toggleEpisodeSheet,
                onToggleLike = viewModel::toggleLike,
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenComments = viewModel::openComments,
                onSelectSpeed = viewModel::selectSpeed,
                onSelectEpisode = viewModel::switchEpisode,
                onCommentLoginRequired = viewModel::onCommentLoginRequired,
                onCommentMessage = viewModel::onCommentMessage,
                onDismissComments = viewModel::closeComments,
                onPlaybackCompleted = onPlaybackCompleted,
            ),
        )
    }

    pendingCommentLoginContext?.let { loginContext ->
        CommentLoginPlaceholderDialog(
            context = loginContext,
            onConfirmLogin = {
                viewModel.restoreCommentSheetAfterLogin()
                pendingCommentLoginContext = null
                Toast.makeText(context, "已回到评论抽屉，请手动重新执行操作", Toast.LENGTH_SHORT).show()
            },
            onDismiss = {
                pendingCommentLoginContext = null
                viewModel.clearPendingCommentLoginContext()
            },
        )
    }
}

@Composable
private fun PlayerStatusScene(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = PlayerPageBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            PlaceholderPlayerHost(
                uiState = PlayerUiState(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                PlayerStatusContent(
                    title = title,
                    message = message,
                    actionLabel = actionLabel,
                    onAction = onAction,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerContent(
    uiState: PlayerUiState,
    callbacks: PlayerContentCallbacks,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = PlayerPageBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            PlaceholderPlayerHost(
                uiState = uiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                PlayerTopBar(
                    episodeLabel = uiState.currentEpisode?.let { "第${it.episodeNumber}集" } ?: "第3集",
                    onBack = callbacks.onBack,
                    onToggleSpeedSheet = callbacks.onToggleSpeedSheet,
                    onToggleMore = callbacks.onToggleMoreSheet,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                PlayerCenterBadge(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 180.dp),
                )

                PlayerRightActionBar(
                    interactionState = uiState.interactionState,
                    favoriteCountLabel = "31.5万",
                    commentCountLabel = "319",
                    likeCountLabel = "1万",
                    shareCountLabel = "3649",
                    onToggleLike = callbacks.onToggleLike,
                    onToggleFavorite = callbacks.onToggleFavorite,
                    onOpenComments = callbacks.onOpenComments,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 2.dp, bottom = 168.dp),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(end = 92.dp, bottom = 82.dp),
                ) {
                    PlayerBottomInfo(
                        title = playerDisplayTitle(uiState),
                        hotComment = "热评：大伯母没错，要不是大伯母...  展开",
                        authorStatement = "作者声明：内容由AI生成",
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PlayerProgressBar(
                        progress = uiState.resumeProgress.toFloat().normalizedPlaybackProgress(),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PlayerEpisodeDock(
                        episodeLabel = "选集·${uiState.seriesStatus.label}·全${uiState.episodes.size.coerceAtLeast(133)}集",
                        onOpenEpisodeSheet = callbacks.onToggleEpisodeSheet,
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                        .size(width = 132.dp, height = 5.dp)
                        .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(100.dp)),
                )

                callbacks.onPlaybackCompleted?.let { completionHandler ->
                    Button(
                        onClick = completionHandler,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp),
                    ) {
                        Text("模拟任务完成")
                    }
                }
            }
        }
    }

    if (uiState.isSpeedSheetVisible) {
        ModalBottomSheet(onDismissRequest = callbacks.onToggleSpeedSheet) {
            SpeedPickerSheetContent(
                speeds = PlaybackSpeed.defaults,
                currentSpeed = uiState.currentSpeed,
                onSelectSpeed = callbacks.onSelectSpeed,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (uiState.isEpisodeSheetVisible) {
        ModalBottomSheet(onDismissRequest = callbacks.onToggleEpisodeSheet) {
            EpisodePickerSheetContent(
                episodes = uiState.episodes,
                currentEpisodeId = uiState.currentEpisode?.id,
                onSelectEpisode = callbacks.onSelectEpisode,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (uiState.commentSheetState.isVisible) {
        CommentBottomSheet(
            dramaId = uiState.commentSheetState.dramaId.orEmpty(),
            source = uiState.commentSheetState.source,
            onDismiss = callbacks.onDismissComments,
            onRequireLogin = { effect -> callbacks.onCommentLoginRequired(effect.context) },
            onMessage = callbacks.onCommentMessage,
        )
    }
}

private fun playerDisplayTitle(uiState: PlayerUiState): String {
    return uiState.currentEpisode?.title.orEmpty().ifBlank { "全族托举农门状元郎 >" }
}

private fun Float.normalizedPlaybackProgress(): Float {
    return when {
        this <= 0f -> 0.56f
        this >= 100f -> 1f
        else -> (this / 100f).coerceIn(0f, 1f)
    }
}

internal fun playerContentVariant(uiState: PlayerUiState): PlayerContentVariant {
    return when (uiState.screenState) {
        PlayerScreenState.IDLE,
        PlayerScreenState.BOOTSTRAPPING,
        -> PlayerContentVariant.LOADING

        PlayerScreenState.ERROR,
        PlayerScreenState.NO_RESOURCE,
        PlayerScreenState.READY,
        PlayerScreenState.PLAYING,
        PlayerScreenState.PAUSED,
        PlayerScreenState.SWITCHING_EPISODE,
        -> PlayerContentVariant.CONTENT
    }
}
