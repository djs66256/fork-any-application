package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.repository.SearchRepository
import javax.inject.Inject

class SearchDramasUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(
        query: String,
        page: Int = 1,
        pageSize: Int = 10,
    ): ApiResult<List<Drama>> = searchRepository.searchDramas(query, page, pageSize)
}
