package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.RankingRemoteDataSource
import com.djs66256.short_drama.data.dto.RankingDramaDto
import com.djs66256.short_drama.domain.model.RankingPage
import com.djs66256.short_drama.domain.model.RankingQuery
import com.djs66256.short_drama.domain.repository.RankingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RankingRepositoryImpl @Inject constructor(
    private val remoteDataSource: RankingRemoteDataSource,
) : RankingRepository {
    override suspend fun getDramaRankings(query: RankingQuery): ApiResult<RankingPage> {
        return when (
            val result = remoteDataSource.getDramaRankings(
                type = query.type.apiValue,
                contentType = query.contentType.apiValue,
                page = query.page,
                pageSize = query.pageSize,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(
                RankingPage(
                    items = result.data.data.map(RankingDramaDto::toDomain),
                    page = result.data.pagination.page,
                    pageSize = result.data.pagination.pageSize,
                    total = result.data.pagination.total,
                    totalPages = result.data.pagination.totalPages,
                ),
            )
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun bookDrama(dramaId: String) = when (val result = remoteDataSource.bookDrama(dramaId)) {
        is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
        is ApiResult.Error -> result
        is ApiResult.Exception -> result
    }
}
