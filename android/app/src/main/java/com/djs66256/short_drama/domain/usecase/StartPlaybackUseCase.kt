package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.StartPlaybackParams
import com.djs66256.short_drama.domain.model.StartPlaybackResult
import com.djs66256.short_drama.domain.repository.PlayerRepository
import javax.inject.Inject

class StartPlaybackUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
) {
    suspend operator fun invoke(params: StartPlaybackParams): ApiResult<StartPlaybackResult> {
        return playerRepository.startPlayback(params)
    }
}
