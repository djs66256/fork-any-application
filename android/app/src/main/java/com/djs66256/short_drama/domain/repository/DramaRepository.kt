package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.BookingAssetsPage
import com.djs66256.short_drama.domain.model.BookingAssetsQuery
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.model.TheaterPage
import com.djs66256.short_drama.domain.model.TheaterQuery

/**
 * Repository interface for drama-related data operations.
 * Defined in the Domain layer, implemented in the Data layer.
 */
interface DramaRepository {
    suspend fun getDramas(page: Int, pageSize: Int): ApiResult<List<Drama>>
    suspend fun getDramaDetail(id: String): ApiResult<Drama>
    suspend fun getTheaterFeed(query: TheaterQuery): ApiResult<TheaterPage>
    suspend fun getBookingAssets(query: BookingAssetsQuery): ApiResult<BookingAssetsPage>
}
