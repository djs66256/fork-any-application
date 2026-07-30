package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.BookingAssetsPage
import com.djs66256.short_drama.domain.model.BookingAssetsQuery
import com.djs66256.short_drama.domain.repository.DramaRepository
import javax.inject.Inject

class GetBookingAssetsUseCase @Inject constructor(
    private val dramaRepository: DramaRepository,
) {
    suspend operator fun invoke(query: BookingAssetsQuery): ApiResult<BookingAssetsPage> {
        return dramaRepository.getBookingAssets(query)
    }
}
