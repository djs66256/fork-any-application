package com.djs66256.short_drama.feature.player.viewmodel

import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.domain.model.SeriesStatus
import com.djs66256.short_drama.feature.comments.model.CommentLoginContext
import com.djs66256.short_drama.feature.comments.model.CommentSource

enum class PlayerScreenState {
    IDLE,
    BOOTSTRAPPING,
    READY,
    PLAYING,
    PAUSED,
    SWITCHING_EPISODE,
    NO_RESOURCE,
    ERROR,
}

enum class PlaybackSpeed(val label: String, val multiplier: Float) {
    X0_5("0.5x", 0.5f),
    X0_75("0.75x", 0.75f),
    X1_0("1.0x", 1.0f),
    X1_25("1.25x", 1.25f),
    X1_5("1.5x", 1.5f),
    X1_75("1.75x", 1.75f),
    X2_0("2.0x", 2.0f),
    ;

    companion object {
        val defaults: List<PlaybackSpeed> = entries
    }
}

data class PlayerInteractionState(
    val liked: Boolean = false,
    val favorited: Boolean = false,
)

data class PlayerCommentSheetState(
    val isVisible: Boolean = false,
    val dramaId: String? = null,
    val source: CommentSource = CommentSource.PLAYER,
)

sealed interface PlayerEffect {
    data class RequireLogin(val context: CommentLoginContext) : PlayerEffect
    data class ShowMessage(val message: String) : PlayerEffect
}

data class PlayerUiState(
    val dramaId: String = "",
    val screenState: PlayerScreenState = PlayerScreenState.IDLE,
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,
    val resumeProgress: Double = 0.0,
    val currentSpeed: PlaybackSpeed = PlaybackSpeed.X1_0,
    val isEpisodeSheetVisible: Boolean = false,
    val isSpeedSheetVisible: Boolean = false,
    val interactionState: PlayerInteractionState = PlayerInteractionState(),
    val commentSheetState: PlayerCommentSheetState = PlayerCommentSheetState(),
    val pendingCommentLoginContext: CommentLoginContext? = null,
    val errorMessage: String? = null,
    val seriesStatus: SeriesStatus = SeriesStatus.COMPLETED,
    val hasLoadedOnce: Boolean = false,
    val isReportingStop: Boolean = false,
) {
    val canRenderPlayerChrome: Boolean
        get() = screenState == PlayerScreenState.READY ||
            screenState == PlayerScreenState.PLAYING ||
            screenState == PlayerScreenState.PAUSED ||
            screenState == PlayerScreenState.SWITCHING_EPISODE

    val playbackTitle: String
        get() = currentEpisode?.let { "第 ${it.episodeNumber} 集 · ${it.title}" } ?: "播放器"
}

internal fun PlayerUiState.primaryDramaTitle(): String {
    return currentEpisode?.title.orEmpty().ifBlank { "短剧播放页" }
}
