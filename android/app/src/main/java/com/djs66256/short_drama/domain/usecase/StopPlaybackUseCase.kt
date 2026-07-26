package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.StopPlaybackParams
import com.djs66256.short_drama.domain.model.StopPlaybackResult
import com.djs66256.short_drama.domain.repository.PlayerRepository
import javax.inject.Inject

class StopPlaybackUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
) {
    suspend operator fun invoke(params: StopPlaybackParams): ApiResult<StopPlaybackResult> {
        return playerRepository.stopPlayback(params)
    }
}
