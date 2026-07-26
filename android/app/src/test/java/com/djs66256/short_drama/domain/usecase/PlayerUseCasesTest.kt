package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.SeriesStatus
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StartPlaybackResult
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackResult
import com.djs66256.short_drama.domain.repository.PlayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerUseCasesTest {

    private val repository = mockk<PlayerRepository>()

    @Test
    fun `T-02 getPlaybackProgress delegates to repository`() = runTest {
        val expected = ApiResult.Success(
            PlaybackProgress(
                dramaId = "drama-1",
                hasHistory = false,
                episodeId = null,
                startTime = 0.0,
                updatedAt = null,
            ),
        )
        coEvery { repository.getPlaybackProgress("drama-1") } returns expected

        val result = GetPlaybackProgressUseCase(repository)("drama-1")

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getPlaybackProgress("drama-1") }
    }

    @Test
    fun `T-02 getDramaEpisodes delegates to repository`() = runTest {
        val expected = ApiResult.Success(
            DramaEpisodeList(
                dramaId = "drama-1",
                seriesStatus = SeriesStatus.COMPLETED,
                items = emptyList(),
            ),
        )
        coEvery { repository.getDramaEpisodes("drama-1") } returns expected

        val result = GetDramaEpisodesUseCase(repository)("drama-1")

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.getDramaEpisodes("drama-1") }
    }

    @Test
    fun `T-02 startPlayback delegates to repository`() = runTest {
        val params = StartPlaybackParams("drama-1", "episode-1", 0.0)
        val expected = ApiResult.Success(
            StartPlaybackResult(
                dramaId = "drama-1",
                episodeId = "episode-1",
                acceptedProgress = 0.0,
                playbackSessionId = "session-123",
                startedAt = "2026-07-26T00:00:00Z",
            ),
        )
        coEvery { repository.startPlayback(params) } returns expected

        val result = StartPlaybackUseCase(repository)(params)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.startPlayback(params) }
    }

    @Test
    fun `T-02 stopPlayback delegates to repository`() = runTest {
        val params = StopPlaybackParams("drama-1", "episode-1", 20.0, 100.0)
        val expected = ApiResult.Success(
            StopPlaybackResult(
                dramaId = "drama-1",
                episodeId = "episode-1",
                savedProgress = 20.0,
                duration = 100.0,
                updatedAt = "2026-07-26T00:00:00Z",
            ),
        )
        coEvery { repository.stopPlayback(params) } returns expected

        val result = StopPlaybackUseCase(repository)(params)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.stopPlayback(params) }
    }
}
