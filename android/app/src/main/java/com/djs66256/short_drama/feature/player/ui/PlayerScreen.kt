package com.djs66256.short_drama.feature.player.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
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
import com.djs66256.short_drama.feature.player.ui.components.PlayerEpisodeDock
import com.djs66256.short_drama.feature.player.ui.components.PlayerRightActionBar
import com.djs66256.short_drama.feature.player.ui.components.PlayerStatusContent
import com.djs66256.short_drama.feature.player.ui.components.PlayerTopBar
import com.djs66256.short_drama.feature.player.ui.components.SpeedPickerSheetContent
import com.djs66256.short_drama.feature.player.viewmodel.PlaybackSpeed
import com.djs66256.short_drama.feature.player.viewmodel.PlayerEffect
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerViewModel

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

    when (playerContentVariant(uiState)) {
        PlayerContentVariant.LOADING -> PlayerStatusContent(
            title = "正在进入播放器",
            message = "按 progress → episodes → start 顺序初始化中...",
            modifier = Modifier.fillMaxSize(),
        )

        PlayerContentVariant.ERROR -> PlayerStatusContent(
            title = "加载失败",
            message = uiState.errorMessage.orEmpty().ifBlank { "加载失败，请重试" },
            actionLabel = "重试",
            onAction = viewModel::retry,
            modifier = Modifier.fillMaxSize(),
        )

        PlayerContentVariant.NO_RESOURCE -> PlayerStatusContent(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerContent(
    uiState: PlayerUiState,
    callbacks: PlayerContentCallbacks,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlayerTopBar(
                title = uiState.playbackTitle,
                currentSpeedLabel = uiState.currentSpeed.label,
                onBack = callbacks.onBack,
                onToggleSpeedSheet = callbacks.onToggleSpeedSheet,
                onToggleMore = callbacks.onToggleMoreSheet,
            )
            PlaceholderPlayerHost(uiState = uiState)
            PlayerRightActionBar(
                interactionState = uiState.interactionState,
                onToggleLike = callbacks.onToggleLike,
                onToggleFavorite = callbacks.onToggleFavorite,
                onOpenComments = callbacks.onOpenComments,
            )
            PlayerBottomInfo(
                title = uiState.currentEpisode?.title ?: "短剧播放页",
                description = uiState.currentEpisode?.description.orEmpty(),
                tag = uiState.seriesStatus.label,
            )
            Spacer(modifier = Modifier.height(4.dp))
            PlayerEpisodeDock(
                episodeCount = uiState.episodes.size,
                currentEpisodeNumber = uiState.currentEpisode?.episodeNumber,
                statusLabel = uiState.seriesStatus.label,
                onOpenEpisodeSheet = callbacks.onToggleEpisodeSheet,
            )
            callbacks.onPlaybackCompleted?.let { completionHandler ->
                Button(onClick = completionHandler) {
                    Text("模拟任务完成")
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

internal fun playerContentVariant(uiState: PlayerUiState): PlayerContentVariant {
    return when (uiState.screenState) {
        PlayerScreenState.IDLE,
        PlayerScreenState.BOOTSTRAPPING,
        -> PlayerContentVariant.LOADING

        PlayerScreenState.ERROR -> PlayerContentVariant.ERROR
        PlayerScreenState.NO_RESOURCE -> PlayerContentVariant.NO_RESOURCE
        PlayerScreenState.READY,
        PlayerScreenState.PLAYING,
        PlayerScreenState.PAUSED,
        PlayerScreenState.SWITCHING_EPISODE,
        -> PlayerContentVariant.CONTENT
    }
}
