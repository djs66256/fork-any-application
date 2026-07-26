package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.repository.PlayerRepository
import javax.inject.Inject

class GetDramaEpisodesUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
) {
    suspend operator fun invoke(dramaId: String): ApiResult<DramaEpisodeList> {
        return playerRepository.getDramaEpisodes(dramaId)
    }
}
