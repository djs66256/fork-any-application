package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.repository.PlayerRepository
import javax.inject.Inject

class GetPlaybackProgressUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
) {
    suspend operator fun invoke(dramaId: String): ApiResult<PlaybackProgress> {
        return playerRepository.getPlaybackProgress(dramaId)
    }
}
