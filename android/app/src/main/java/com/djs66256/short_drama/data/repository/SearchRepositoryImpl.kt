package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.SearchRemoteDataSource
import com.djs66256.short_drama.data.dto.DramaDto
import com.djs66256.short_drama.data.local.SearchHistoryLocalDataSource
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.model.HotSearchItem
import com.djs66256.short_drama.domain.model.SearchHistoryItem
import com.djs66256.short_drama.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val remoteDataSource: SearchRemoteDataSource,
    private val localDataSource: SearchHistoryLocalDataSource,
) : SearchRepository {
    override suspend fun searchDramas(
        query: String,
        page: Int,
        pageSize: Int,
    ): ApiResult<List<Drama>> {
        return when (val result = remoteDataSource.searchDramas(query, page, pageSize)) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map(DramaDto::toDomain))
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getHotSearches(): ApiResult<List<HotSearchItem>> {
        return when (val result = remoteDataSource.getHotSearches()) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.map { it.toDomain() })
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override fun observeSearchHistory(): Flow<List<SearchHistoryItem>> = localDataSource.history

    override suspend fun saveSearchHistory(keyword: String) {
        localDataSource.save(keyword)
    }

    override suspend fun clearSearchHistory() {
        localDataSource.clear()
    }
}
