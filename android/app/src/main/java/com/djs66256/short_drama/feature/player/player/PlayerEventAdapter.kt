package com.djs66256.short_drama.feature.player.player

data class PlayerEventAdapter(
    val onPlayingChanged: (Boolean) -> Unit = {},
    val onPositionChanged: (Double) -> Unit = {},
    val onPlaybackError: (String) -> Unit = {},
)
