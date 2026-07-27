package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.HotSearchItem
import com.djs66256.short_drama.domain.repository.SearchRepository
import javax.inject.Inject

class GetHotSearchKeywordsUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(): ApiResult<List<HotSearchItem>> = searchRepository.getHotSearches()
}
