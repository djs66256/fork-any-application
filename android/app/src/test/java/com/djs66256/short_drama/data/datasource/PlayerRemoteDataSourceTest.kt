package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.EpisodeDto
import com.djs66256.short_drama.data.dto.EpisodeListDataDto
import com.djs66256.short_drama.data.dto.EpisodeListResponseDto
import com.djs66256.short_drama.data.dto.PlayerProgressDataDto
import com.djs66256.short_drama.data.dto.PlayerProgressResponseDto
import com.djs66256.short_drama.data.dto.PlayerStartDataDto
import com.djs66256.short_drama.data.dto.PlayerStartRequestDto
import com.djs66256.short_drama.data.dto.PlayerStartResponseDto
import com.djs66256.short_drama.data.dto.PlayerStopDataDto
import com.djs66256.short_drama.data.dto.PlayerStopRequestDto
import com.djs66256.short_drama.data.dto.PlayerStopResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRemoteDataSourceTest {

    private val apiService = mockk<ApiService>()
    private val dataSource = PlayerRemoteDataSource(apiService)

    @Test
    fun `T-01 progress start stop forward playback session header but episodes does not`() = runTest {
        val sessionId = "session-123"
        val dramaId = "drama-123"
        val startRequest = PlayerStartRequestDto(
            dramaId = dramaId,
            episodeId = "episode-1",
            progress = 12.0,
        )
        val stopRequest = PlayerStopRequestDto(
            dramaId = dramaId,
            episodeId = "episode-1",
            progress = 18.0,
            duration = 80.0,
        )

        coEvery {
            apiService.getPlaybackProgress(playbackSessionId = sessionId, dramaId = dramaId)
        } returns sampleProgressResponse()
        coEvery {
            apiService.getDramaEpisodes(dramaId = dramaId)
        } returns sampleEpisodeListResponse(dramaId)
        coEvery {
            apiService.startPlayback(playbackSessionId = sessionId, body = startRequest)
        } returns sampleStartResponse(dramaId)
        coEvery {
            apiService.stopPlayback(playbackSessionId = sessionId, body = stopRequest)
        } returns sampleStopResponse(dramaId)

        assertTrue(dataSource.getPlaybackProgress(sessionId, dramaId) is ApiResult.Success)
        assertTrue(dataSource.getDramaEpisodes(dramaId) is ApiResult.Success)
        assertTrue(dataSource.startPlayback(sessionId, startRequest) is ApiResult.Success)
        assertTrue(dataSource.stopPlayback(sessionId, stopRequest) is ApiResult.Success)

        coVerify(exactly = 1) { apiService.getPlaybackProgress(playbackSessionId = sessionId, dramaId = dramaId) }
        coVerify(exactly = 1) { apiService.getDramaEpisodes(dramaId = dramaId) }
        coVerify(exactly = 1) { apiService.startPlayback(playbackSessionId = sessionId, body = startRequest) }
        coVerify(exactly = 1) { apiService.stopPlayback(playbackSessionId = sessionId, body = stopRequest) }
    }

    @Test
    fun `T-01 progress maps empty history payload for bootstrap`() = runTest {
        val sessionId = "session-123"
        val dramaId = "drama-123"
        coEvery {
            apiService.getPlaybackProgress(playbackSessionId = sessionId, dramaId = dramaId)
        } returns PlayerProgressResponseDto(
            data = PlayerProgressDataDto(
                dramaId = dramaId,
                hasHistory = false,
                episodeId = null,
                startTime = 0.0,
                updatedAt = null,
            ),
        )

        val result = dataSource.getPlaybackProgress(sessionId, dramaId)

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data.data
        assertEquals(false, data.hasHistory)
        assertEquals(null, data.episodeId)
        assertEquals(0.0, data.startTime, 0.0)
    }

    private fun sampleProgressResponse() = PlayerProgressResponseDto(
        data = PlayerProgressDataDto(
            dramaId = "drama-123",
            hasHistory = true,
            episodeId = "episode-2",
            startTime = 15.0,
            updatedAt = "2026-07-26T00:00:00Z",
        ),
    )

    private fun sampleEpisodeListResponse(dramaId: String) = EpisodeListResponseDto(
        data = EpisodeListDataDto(
            dramaId = dramaId,
            seriesStatus = "completed",
            items = listOf(
                EpisodeDto(
                    id = "episode-1",
                    dramaId = dramaId,
                    title = "第 1 集",
                    episodeNumber = 1,
                    videoUrl = "https://example.com/1.mp4",
                    duration = 100,
                    thumbnailUrl = "https://example.com/1.jpg",
                    createdAt = "2026-07-26T00:00:00Z",
                    updatedAt = "2026-07-26T00:00:00Z",
                ),
            ),
        ),
    )

    private fun sampleStartResponse(dramaId: String) = PlayerStartResponseDto(
        data = PlayerStartDataDto(
            dramaId = dramaId,
            episodeId = "episode-1",
            acceptedProgress = 12.0,
            playbackSessionId = "session-123",
            startedAt = "2026-07-26T00:00:00Z",
        ),
    )

    private fun sampleStopResponse(dramaId: String) = PlayerStopResponseDto(
        data = PlayerStopDataDto(
            dramaId = dramaId,
            episodeId = "episode-1",
            savedProgress = 18.0,
            duration = 80.0,
            updatedAt = "2026-07-26T00:00:00Z",
        ),
    )
}
