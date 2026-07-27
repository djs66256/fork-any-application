package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.RankingPage
import com.djs66256.short_drama.domain.model.RankingQuery
import com.djs66256.short_drama.domain.repository.RankingRepository
import javax.inject.Inject

class GetDramaRankingsUseCase @Inject constructor(
    private val rankingRepository: RankingRepository,
) {
    suspend operator fun invoke(query: RankingQuery): ApiResult<RankingPage> {
        return rankingRepository.getDramaRankings(query)
    }
}
