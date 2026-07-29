package com.djs66256.short_drama.feature.player.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.model.Episode
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.SeriesStatus
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StartPlaybackResult
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackResult
import com.djs66256.short_drama.domain.usecase.GetDramaEpisodesUseCase
import com.djs66256.short_drama.domain.usecase.GetPlaybackProgressUseCase
import com.djs66256.short_drama.domain.usecase.StartPlaybackUseCase
import com.djs66256.short_drama.domain.usecase.StopPlaybackUseCase
import com.djs66256.short_drama.feature.comments.model.CommentPendingActionType
import com.djs66256.short_drama.feature.comments.model.CommentSource
import com.djs66256.short_drama.feature.comments.model.PendingCommentAction
import com.djs66256.short_drama.feature.comments.model.buildCommentLoginContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getPlaybackProgressUseCase = mockk<GetPlaybackProgressUseCase>()
    private val getDramaEpisodesUseCase = mockk<GetDramaEpisodesUseCase>()
    private val startPlaybackUseCase = mockk<StartPlaybackUseCase>()
    private val stopPlaybackUseCase = mockk<StopPlaybackUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-03 bootstrap without history starts first playable episode from zero`() = runTest {
        stubBootstrap(
            progress = PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = false,
                episodeId = null,
                startTime = 0.0,
                updatedAt = null,
            ),
            episodes = DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.COMPLETED,
                items = listOf(unplayableEpisode(1), playableEpisode(2)),
            ),
        )
        coEvery {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-2", 0.0))
        } returns ApiResult.Success(startResult("episode-2", 0.0))

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-1")))
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("drama-1", viewModel.dramaId)
        assertEquals(PlayerScreenState.PLAYING, state.screenState)
        assertEquals("episode-2", state.currentEpisode?.id)
        assertEquals(0.0, state.resumeProgress, 0.0)
        coVerifyOrderForBootstrap("episode-2", 0.0)
    }

    @Test
    fun `T-04 bootstrap restores history when recovered episode is playable`() = runTest {
        stubBootstrap(
            progress = PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = true,
                episodeId = "episode-3",
                startTime = 45.0,
                updatedAt = "2026-07-26T00:00:00Z",
            ),
            episodes = DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.ONGOING,
                items = listOf(playableEpisode(1), playableEpisode(3)),
            ),
        )
        coEvery {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-3", 45.0))
        } returns ApiResult.Success(startResult("episode-3", 45.0))

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-1")))
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("episode-3", state.currentEpisode?.id)
        assertEquals(45.0, state.resumeProgress, 0.0)
        assertEquals(SeriesStatus.ONGOING, state.seriesStatus)
    }

    @Test
    fun `T-04 bootstrap falls back to first playable episode when restored episode is missing`() = runTest {
        stubBootstrap(
            progress = PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = true,
                episodeId = "missing-episode",
                startTime = 30.0,
                updatedAt = "2026-07-26T00:00:00Z",
            ),
            episodes = DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.COMPLETED,
                items = listOf(playableEpisode(1), playableEpisode(2)),
            ),
        )
        coEvery {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-1", 0.0))
        } returns ApiResult.Success(startResult("episode-1", 0.0))

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-1")))
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("episode-1", state.currentEpisode?.id)
        assertEquals(0.0, state.resumeProgress, 0.0)
    }

    @Test
    fun `T-04 bootstrap enters no-resource when no playable episodes exist`() = runTest {
        stubBootstrap(
            progress = PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = false,
                episodeId = null,
                startTime = 0.0,
                updatedAt = null,
            ),
            episodes = DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.COMPLETED,
                items = listOf(unplayableEpisode(1), unplayableEpisode(2)),
            ),
        )

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-1")))
        viewModel.loadIfNeeded()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PlayerScreenState.NO_RESOURCE, state.screenState)
        assertEquals(null, state.currentEpisode)
    }

    @Test
    fun `T-05 switchEpisode stops current episode then starts next from zero while keeping speed`() = runTest {
        stubBootstrap(
            progress = PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = false,
                episodeId = null,
                startTime = 0.0,
                updatedAt = null,
            ),
            episodes = DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.COMPLETED,
                items = listOf(playableEpisode(1), playableEpisode(2)),
            ),
        )
        coEvery {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-1", 0.0))
        } returns ApiResult.Success(startResult("episode-1", 0.0))
        coEvery {
            stopPlaybackUseCase(StopPlaybackParams("drama-1", "episode-1", 20.0, 100.0))
        } returns ApiResult.Success(stopResult("episode-1", 20.0, 100.0))
        coEvery {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-2", 0.0))
        } returns ApiResult.Success(startResult("episode-2", 0.0))

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-1")))
        viewModel.loadIfNeeded()
        advanceUntilIdle()
        viewModel.selectSpeed(PlaybackSpeed.X1_5)
        viewModel.onPlaybackPositionChanged(20.0)

        viewModel.switchEpisode(playableEpisode(2))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PlayerScreenState.PLAYING, state.screenState)
        assertEquals("episode-2", state.currentEpisode?.id)
        assertEquals(PlaybackSpeed.X1_5, state.currentSpeed)
        assertEquals(0.0, state.resumeProgress, 0.0)
        coVerify(exactly = 1) {
            stopPlaybackUseCase(StopPlaybackParams("drama-1", "episode-1", 20.0, 100.0))
        }
        coVerify(exactly = 1) {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-2", 0.0))
        }
    }

    @Test
    fun `T-06 lifecycle background pauses state and reports stop best-effort`() = runTest {
        stubBootstrap(
            progress = PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = false,
                episodeId = null,
                startTime = 0.0,
                updatedAt = null,
            ),
            episodes = DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.COMPLETED,
                items = listOf(playableEpisode(1)),
            ),
        )
        coEvery {
            startPlaybackUseCase(StartPlaybackParams("drama-1", "episode-1", 0.0))
        } returns ApiResult.Success(startResult("episode-1", 0.0))
        coEvery {
            stopPlaybackUseCase(StopPlaybackParams("drama-1", "episode-1", 12.0, 100.0))
        } returns ApiResult.Exception(IllegalStateException("network"))

        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-1")))
        viewModel.loadIfNeeded()
        advanceUntilIdle()
        viewModel.onPlaybackPositionChanged(12.0)

        viewModel.onBackgrounded()
        advanceUntilIdle()

        assertEquals(PlayerScreenState.PAUSED, viewModel.uiState.value.screenState)
        coVerify(exactly = 1) {
            stopPlaybackUseCase(StopPlaybackParams("drama-1", "episode-1", 12.0, 100.0))
        }
    }

    @Test
    fun `T-07 player comment entry opens and closes sheet with current drama`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-xyz")))

        viewModel.openComments()
        assertTrue(viewModel.uiState.value.commentSheetState.isVisible)
        assertEquals("drama-xyz", viewModel.uiState.value.commentSheetState.dramaId)

        viewModel.closeComments()
        assertFalse(viewModel.uiState.value.commentSheetState.isVisible)
    }

    @Test
    fun `T-07 player stores comment login context and restores sheet without replay`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-xyz")))
        val context = buildCommentLoginContext(
            source = CommentSource.PLAYER,
            dramaId = "drama-xyz",
            action = PendingCommentAction(
                type = CommentPendingActionType.TOGGLE_LIKE,
                commentId = "comment-1",
            ),
        )

        viewModel.effects.test {
            viewModel.onCommentLoginRequired(context)
            val effect = awaitItem() as PlayerEffect.RequireLogin
            assertEquals("play/drama-xyz", effect.context.returnRoute)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(context, viewModel.uiState.value.pendingCommentLoginContext)
        viewModel.restoreCommentSheetAfterLogin()
        assertTrue(viewModel.uiState.value.commentSheetState.isVisible)
        assertEquals("drama-xyz", viewModel.uiState.value.commentSheetState.dramaId)
        assertEquals(null, viewModel.uiState.value.pendingCommentLoginContext)
    }

    @Test
    fun `T-08 route arg internally maps videoId to dramaId`() = runTest {
        val viewModel = createViewModel(SavedStateHandle(mapOf("videoId" to "drama-xyz")))
        assertEquals("drama-xyz", viewModel.dramaId)
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle): PlayerViewModel {
        return PlayerViewModel(
            savedStateHandle = savedStateHandle,
            getPlaybackProgressUseCase = getPlaybackProgressUseCase,
            getDramaEpisodesUseCase = getDramaEpisodesUseCase,
            startPlaybackUseCase = startPlaybackUseCase,
            stopPlaybackUseCase = stopPlaybackUseCase,
        )
    }

    private fun stubBootstrap(
        progress: PlaybackProgress,
        episodes: DramaEpisodeList,
    ) {
        coEvery { getPlaybackProgressUseCase("drama-1") } returns ApiResult.Success(progress)
        coEvery { getDramaEpisodesUseCase("drama-1") } returns ApiResult.Success(episodes)
    }

    private fun coVerifyOrderForBootstrap(targetEpisodeId: String, progress: Double) {
        coVerifyOrder {
            getPlaybackProgressUseCase("drama-1")
            getDramaEpisodesUseCase("drama-1")
            startPlaybackUseCase(StartPlaybackParams("drama-1", targetEpisodeId, progress))
        }
    }

    private fun playableEpisode(number: Int): Episode = Episode(
        id = "episode-$number",
        dramaId = "drama-1",
        title = "第 $number 集",
        episodeNumber = number,
        videoUrl = "https://example.com/$number.mp4",
        duration = 100,
        thumbnailUrl = "https://example.com/$number.jpg",
        description = "第 $number 集简介",
        createdAt = "2026-07-26T00:00:00Z",
        updatedAt = "2026-07-26T00:00:00Z",
    )

    private fun unplayableEpisode(number: Int): Episode = Episode(
        id = "episode-$number",
        dramaId = "drama-1",
        title = "第 $number 集",
        episodeNumber = number,
        videoUrl = "",
        duration = 100,
        thumbnailUrl = "",
        description = "第 $number 集简介",
        createdAt = "2026-07-26T00:00:00Z",
        updatedAt = "2026-07-26T00:00:00Z",
    )

    private fun startResult(episodeId: String, progress: Double): StartPlaybackResult = StartPlaybackResult(
        dramaId = "drama-1",
        episodeId = episodeId,
        acceptedProgress = progress,
        playbackSessionId = "session-123",
        startedAt = "2026-07-26T00:00:00Z",
    )

    private fun stopResult(
        episodeId: String,
        progress: Double,
        duration: Double,
    ): StopPlaybackResult = StopPlaybackResult(
        dramaId = "drama-1",
        episodeId = episodeId,
        savedProgress = progress,
        duration = duration,
        updatedAt = "2026-07-26T00:00:00Z",
    )
}
