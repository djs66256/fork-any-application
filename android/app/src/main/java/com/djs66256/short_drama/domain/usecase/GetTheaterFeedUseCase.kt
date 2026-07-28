package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.TheaterPage
import com.djs66256.short_drama.domain.model.TheaterQuery
import com.djs66256.short_drama.domain.repository.DramaRepository
import javax.inject.Inject

class GetTheaterFeedUseCase @Inject constructor(
    private val dramaRepository: DramaRepository,
) {
    suspend operator fun invoke(query: TheaterQuery): ApiResult<TheaterPage> {
        return dramaRepository.getTheaterFeed(query)
    }
}
