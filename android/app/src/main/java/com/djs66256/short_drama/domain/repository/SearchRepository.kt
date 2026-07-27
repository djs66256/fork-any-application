package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Drama
import com.djs66256.short_drama.domain.model.HotSearchItem
import com.djs66256.short_drama.domain.model.SearchHistoryItem
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun searchDramas(
        query: String,
        page: Int = 1,
        pageSize: Int = 10,
    ): ApiResult<List<Drama>>

    suspend fun getHotSearches(): ApiResult<List<HotSearchItem>>

    fun observeSearchHistory(): Flow<List<SearchHistoryItem>>

    suspend fun saveSearchHistory(keyword: String)

    suspend fun clearSearchHistory()
}
