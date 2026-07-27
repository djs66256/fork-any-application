package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.BookDramaResult
import com.djs66256.short_drama.domain.repository.RankingRepository
import javax.inject.Inject

class BookDramaUseCase @Inject constructor(
    private val rankingRepository: RankingRepository,
) {
    suspend operator fun invoke(dramaId: String): ApiResult<BookDramaResult> {
        return rankingRepository.bookDrama(dramaId)
    }
}
