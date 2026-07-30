package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.DramaRemoteDataSource
import com.djs66256.short_drama.data.dto.DramaDto
import com.djs66256.short_drama.domain.model.BookingAssetsPage
import com.djs66256.short_drama.domain.model.BookingAssetsQuery
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.model.TheaterPage
import com.djs66256.short_drama.domain.model.TheaterQuery
import com.djs66256.short_drama.domain.repository.DramaRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [DramaRepository] that fetches data from the remote API
 * via [DramaRemoteDataSource] and maps DTOs to domain models.
 */
@Singleton
class DramaRepositoryImpl @Inject constructor(
    private val remoteDataSource: DramaRemoteDataSource,
) : DramaRepository {

    override suspend fun getDramas(page: Int, pageSize: Int): ApiResult<List<Drama>> {
        return when (val result = remoteDataSource.getDramas(page, pageSize)) {
            is ApiResult.Success -> {
                val domainDramas = result.data.data.map(DramaDto::toDomain)
                ApiResult.Success(domainDramas)
            }
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getDramaDetail(id: String): ApiResult<Drama> {
        return when (val result = remoteDataSource.getDramaDetail(id)) {
            is ApiResult.Success -> ApiResult.Exception(
                UnsupportedOperationException("getDramaDetail DTO mapping not yet implemented")
            )
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getTheaterFeed(query: TheaterQuery): ApiResult<TheaterPage> {
        return when (
            val result = remoteDataSource.getTheaterFeed(
                channel = query.channel.apiValue,
                page = query.page,
                pageSize = query.pageSize,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(channel = query.channel))
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getBookingAssets(query: BookingAssetsQuery): ApiResult<BookingAssetsPage> {
        return when (
            val result = remoteDataSource.getUserBookings(
                status = query.status.apiValue,
                page = query.page,
                pageSize = query.pageSize,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }
}
