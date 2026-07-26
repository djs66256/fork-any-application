package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.EpisodeListResponseDto
import com.djs66256.short_drama.data.dto.PlayerProgressResponseDto
import com.djs66256.short_drama.data.dto.PlayerStartRequestDto
import com.djs66256.short_drama.data.dto.PlayerStartResponseDto
import com.djs66256.short_drama.data.dto.PlayerStopRequestDto
import com.djs66256.short_drama.data.dto.PlayerStopResponseDto
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun getPlaybackProgress(
        playbackSessionId: String,
        dramaId: String,
    ): ApiResult<PlayerProgressResponseDto> {
        return try {
            ApiResult.Success(apiService.getPlaybackProgress(playbackSessionId = playbackSessionId, dramaId = dramaId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }

    suspend fun getDramaEpisodes(dramaId: String): ApiResult<EpisodeListResponseDto> {
        return try {
            ApiResult.Success(apiService.getDramaEpisodes(dramaId = dramaId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }

    suspend fun startPlayback(
        playbackSessionId: String,
        request: PlayerStartRequestDto,
    ): ApiResult<PlayerStartResponseDto> {
        return try {
            ApiResult.Success(apiService.startPlayback(playbackSessionId = playbackSessionId, body = request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }

    suspend fun stopPlayback(
        playbackSessionId: String,
        request: PlayerStopRequestDto,
    ): ApiResult<PlayerStopResponseDto> {
        return try {
            ApiResult.Success(apiService.stopPlayback(playbackSessionId = playbackSessionId, body = request))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }
}
