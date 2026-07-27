package com.djs66256.short_drama.data.repository

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.SearchRemoteDataSource
import com.djs66256.short_drama.data.dto.DramaDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.HotSearchItemDto
import com.djs66256.short_drama.data.dto.HotSearchListResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.local.SearchHistoryLocalDataSource
import com.djs66256.short_drama.domain.model.SearchHistoryItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRepositoryImplTest {
    private val remoteDataSource = mockk<SearchRemoteDataSource>()
    private val localDataSource = mockk<SearchHistoryLocalDataSource>()
    private val repository = SearchRepositoryImpl(remoteDataSource, localDataSource)

    @Test
    fun `T-06 searchDramas maps dto into domain dramas`() = runTest {
        val response = DramaListResponseDto(
            data = listOf(
                DramaDto(
                    id = "drama-1",
                    title = "逆袭人生",
                    description = "desc",
                    coverUrl = "cover",
                    category = "都市",
                    episodeCount = 10,
                    tags = listOf("逆袭"),
                    rating = 8.2,
                    createdAt = "2026-07-25T00:00:00Z",
                    updatedAt = "2026-07-25T00:00:00Z",
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 10, total = 1, totalPages = 1),
        )
        coEvery { remoteDataSource.searchDramas("逆袭", 1, 10) } returns ApiResult.Success(response)

        val result = repository.searchDramas("逆袭", 1, 10)

        assertTrue(result is ApiResult.Success)
        assertEquals("逆袭人生", (result as ApiResult.Success).data.single().title)
    }

    @Test
    fun `T-06 getHotSearches maps dto into domain hot search items`() = runTest {
        coEvery { remoteDataSource.getHotSearches() } returns ApiResult.Success(
            HotSearchListResponseDto(
                data = listOf(HotSearchItemDto(rank = 1, keyword = "逆袭", score = 999)),
            ),
        )

        val result = repository.getHotSearches()

        assertTrue(result is ApiResult.Success)
        assertEquals("逆袭", (result as ApiResult.Success).data.single().keyword)
    }

    @Test
    fun `T-05 observe and clear history delegate to local data source`() = runTest {
        val history = listOf(SearchHistoryItem(keyword = "逆袭", updatedAtEpochMillis = 1L))
        every { localDataSource.history } returns flowOf(history)
        coEvery { localDataSource.clear() } returns Unit

        repository.observeSearchHistory().test {
            assertEquals(history, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        repository.clearSearchHistory()

        coVerify(exactly = 1) { localDataSource.clear() }
    }
}
