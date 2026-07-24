package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.repository.DramaRepository
import javax.inject.Inject

/**
 * Use case for fetching a paginated list of dramas.
 * Delegates to [DramaRepository] and adds no additional business logic at this stage.
 */
class GetDramasUseCase @Inject constructor(
    private val dramaRepository: DramaRepository
) {
    suspend operator fun invoke(
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResult<List<Drama>> {
        return dramaRepository.getDramas(page, pageSize)
    }
}
