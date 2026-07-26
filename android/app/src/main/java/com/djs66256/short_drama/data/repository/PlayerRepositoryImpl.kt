package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.PlaybackSessionStore
import com.djs66256.short_drama.data.datasource.PlayerRemoteDataSource
import com.djs66256.short_drama.data.dto.PlayerStartRequestDto
import com.djs66256.short_drama.data.dto.PlayerStopRequestDto
import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StartPlaybackResult
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackResult
import com.djs66256.short_drama.domain.repository.PlayerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val remoteDataSource: PlayerRemoteDataSource,
    private val playbackSessionStore: PlaybackSessionStore,
) : PlayerRepository {

    override suspend fun getPlaybackProgress(dramaId: String): ApiResult<PlaybackProgress> {
        val playbackSessionId = playbackSessionStore.getOrCreateSessionId()
        return when (
            val result = remoteDataSource.getPlaybackProgress(
                playbackSessionId = playbackSessionId,
                dramaId = dramaId,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getDramaEpisodes(dramaId: String): ApiResult<DramaEpisodeList> {
        return when (val result = remoteDataSource.getDramaEpisodes(dramaId = dramaId)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun startPlayback(params: StartPlaybackParams): ApiResult<StartPlaybackResult> {
        val playbackSessionId = playbackSessionStore.getOrCreateSessionId()
        val request = PlayerStartRequestDto(
            dramaId = params.dramaId,
            episodeId = params.episodeId,
            progress = params.progress,
        )

        return when (
            val result = remoteDataSource.startPlayback(
                playbackSessionId = playbackSessionId,
                request = request,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun stopPlayback(params: StopPlaybackParams): ApiResult<StopPlaybackResult> {
        val playbackSessionId = playbackSessionStore.getOrCreateSessionId()
        val request = PlayerStopRequestDto(
            dramaId = params.dramaId,
            episodeId = params.episodeId,
            progress = params.progress,
            duration = params.duration,
        )

        return when (
            val result = remoteDataSource.stopPlayback(
                playbackSessionId = playbackSessionId,
                request = request,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }
}
