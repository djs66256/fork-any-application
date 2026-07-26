package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StartPlaybackResult
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackResult

interface PlayerRepository {
    suspend fun getPlaybackProgress(dramaId: String): ApiResult<PlaybackProgress>
    suspend fun getDramaEpisodes(dramaId: String): ApiResult<DramaEpisodeList>
    suspend fun startPlayback(params: StartPlaybackParams): ApiResult<StartPlaybackResult>
    suspend fun stopPlayback(params: StopPlaybackParams): ApiResult<StopPlaybackResult>
}
