package com.djs66256.short_drama.feature.player.ui

import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.domain.model.SeriesStatus
import com.djs66256.short_drama.feature.player.viewmodel.PlaybackSpeed
import com.djs66256.short_drama.feature.player.viewmodel.PlayerInteractionState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerScreenState
import com.djs66256.short_drama.feature.player.viewmodel.PlayerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerScreenStateTest {

    @Test
    fun `T-07 loading error no-resource and ready states map to expected content variants`() {
        assertEquals(
            PlayerContentVariant.LOADING,
            playerContentVariant(PlayerUiState(screenState = PlayerScreenState.BOOTSTRAPPING)),
        )
        assertEquals(
            PlayerContentVariant.ERROR,
            playerContentVariant(PlayerUiState(screenState = PlayerScreenState.ERROR, errorMessage = "boom")),
        )
        assertEquals(
            PlayerContentVariant.NO_RESOURCE,
            playerContentVariant(PlayerUiState(screenState = PlayerScreenState.NO_RESOURCE)),
        )
        assertEquals(
            PlayerContentVariant.CONTENT,
            playerContentVariant(PlayerUiState(screenState = PlayerScreenState.READY)),
        )
    }

    @Test
    fun `T-07 player ui state preserves sheet toggles speed and interaction flags`() {
        val state = PlayerUiState(
            screenState = PlayerScreenState.PLAYING,
            currentSpeed = PlaybackSpeed.X1_5,
            isEpisodeSheetVisible = true,
            isSpeedSheetVisible = true,
            interactionState = PlayerInteractionState(liked = true, favorited = true),
            episodes = listOf(sampleEpisode()),
            currentEpisode = sampleEpisode(),
            seriesStatus = SeriesStatus.ONGOING,
        )

        assertTrue(state.canRenderPlayerChrome)
        assertEquals("1.5x", state.currentSpeed.label)
        assertTrue(state.isEpisodeSheetVisible)
        assertTrue(state.isSpeedSheetVisible)
        assertTrue(state.interactionState.liked)
        assertTrue(state.interactionState.favorited)
        assertEquals("第 1 集 · 第 1 集", state.playbackTitle)
    }

    @Test
    fun `T-09 placeholder-only player state remains renderable without media dependency`() {
        val state = PlayerUiState(
            dramaId = "drama-1",
            screenState = PlayerScreenState.PAUSED,
            episodes = listOf(sampleEpisode()),
            currentEpisode = sampleEpisode(),
            resumeProgress = 12.0,
        )

        assertEquals(PlayerContentVariant.CONTENT, playerContentVariant(state))
        assertFalse(state.currentEpisode?.videoUrl.isNullOrBlank())
    }

    private fun sampleEpisode() = Episode(
        id = "episode-1",
        dramaId = "drama-1",
        title = "第 1 集",
        episodeNumber = 1,
        videoUrl = "https://example.com/video.mp4",
        duration = 120,
        thumbnailUrl = "https://example.com/thumb.jpg",
        description = "简介",
        createdAt = "2026-07-26T00:00:00Z",
        updatedAt = "2026-07-26T00:00:00Z",
    )
}
