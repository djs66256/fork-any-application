package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.PlaybackSessionStore
import com.djs66256.short_drama.data.datasource.PlayerRemoteDataSource
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
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerRepositoryImplTest {

    private val remoteDataSource = mockk<PlayerRemoteDataSource>()
    private val playbackSessionStore = mockk<PlaybackSessionStore>()
    private val repository = PlayerRepositoryImpl(remoteDataSource, playbackSessionStore)

    @Test
    fun `T-02 repository reuses session id for progress and playback commands`() = runTest {
        coEvery { playbackSessionStore.getOrCreateSessionId() } returns "session-123"
        coEvery {
            remoteDataSource.getPlaybackProgress(playbackSessionId = "session-123", dramaId = "drama-1")
        } returns ApiResult.Success(
            PlayerProgressResponseDto(
                data = PlayerProgressDataDto(
                    dramaId = "drama-1",
                    hasHistory = true,
                    episodeId = "episode-2",
                    startTime = 22.0,
                    updatedAt = "2026-07-26T00:00:00Z",
                ),
            ),
        )
        coEvery {
            remoteDataSource.startPlayback(
                playbackSessionId = "session-123",
                request = PlayerStartRequestDto(dramaId = "drama-1", episodeId = "episode-2", progress = 22.0),
            )
        } returns ApiResult.Success(
            PlayerStartResponseDto(
                data = PlayerStartDataDto(
                    dramaId = "drama-1",
                    episodeId = "episode-2",
                    acceptedProgress = 22.0,
                    playbackSessionId = "session-123",
                    startedAt = "2026-07-26T00:00:00Z",
                ),
            ),
        )
        coEvery {
            remoteDataSource.stopPlayback(
                playbackSessionId = "session-123",
                request = PlayerStopRequestDto(
                    dramaId = "drama-1",
                    episodeId = "episode-2",
                    progress = 30.0,
                    duration = 90.0,
                ),
            )
        } returns ApiResult.Success(
            PlayerStopResponseDto(
                data = PlayerStopDataDto(
                    dramaId = "drama-1",
                    episodeId = "episode-2",
                    savedProgress = 30.0,
                    duration = 90.0,
                    updatedAt = "2026-07-26T00:00:00Z",
                ),
            ),
        )

        val progressResult = repository.getPlaybackProgress("drama-1")
        val startResult = repository.startPlayback(StartPlaybackParams("drama-1", "episode-2", 22.0))
        val stopResult = repository.stopPlayback(StopPlaybackParams("drama-1", "episode-2", 30.0, 90.0))

        assertTrue(progressResult is ApiResult.Success)
        assertEquals("episode-2", (progressResult as ApiResult.Success).data.episodeId)
        assertTrue(startResult is ApiResult.Success)
        assertEquals(22.0, (startResult as ApiResult.Success).data.acceptedProgress, 0.0)
        assertTrue(stopResult is ApiResult.Success)
        assertEquals(30.0, (stopResult as ApiResult.Success).data.savedProgress, 0.0)

        coVerify(exactly = 3) { playbackSessionStore.getOrCreateSessionId() }
    }

    @Test
    fun `T-02 episodes request skips playback session header dependency and maps sorted episodes`() = runTest {
        coEvery {
            remoteDataSource.getDramaEpisodes(dramaId = "drama-1")
        } returns ApiResult.Success(
            EpisodeListResponseDto(
                data = EpisodeListDataDto(
                    dramaId = "drama-1",
                    seriesStatus = "ongoing",
                    items = listOf(
                        EpisodeDto(
                            id = "episode-2",
                            dramaId = "drama-1",
                            title = "第 2 集",
                            episodeNumber = 2,
                            videoUrl = "https://example.com/2.mp4",
                            duration = 100,
                            thumbnailUrl = "",
                            createdAt = "2026-07-26T00:00:00Z",
                            updatedAt = "2026-07-26T00:00:00Z",
                        ),
                        EpisodeDto(
                            id = "episode-1",
                            dramaId = "drama-1",
                            title = "第 1 集",
                            episodeNumber = 1,
                            videoUrl = "https://example.com/1.mp4",
                            duration = 100,
                            thumbnailUrl = "",
                            createdAt = "2026-07-26T00:00:00Z",
                            updatedAt = "2026-07-26T00:00:00Z",
                        ),
                    ),
                ),
            ),
        )

        val result = repository.getDramaEpisodes("drama-1")

        assertTrue(result is ApiResult.Success)
        val episodes = (result as ApiResult.Success).data
        assertEquals("ongoing", episodes.seriesStatus.wireValue)
        assertEquals(listOf("episode-1", "episode-2"), episodes.items.map { it.id })
    }
}
