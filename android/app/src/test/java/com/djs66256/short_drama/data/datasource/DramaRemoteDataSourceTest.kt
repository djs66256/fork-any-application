package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.dto.TheaterDramaDto
import com.djs66256.short_drama.data.dto.TheaterFeedResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DramaRemoteDataSourceTest {

    private val apiService = mockk<ApiService>()
    private val dataSource = DramaRemoteDataSource(apiService)

    @Test
    fun `T-12 getTheaterFeed returns success for normal payload`() = runTest {
        val response = TheaterFeedResponseDto(
            data = listOf(
                TheaterDramaDto(
                    id = "drama-1",
                    title = "剧场示例",
                    description = "desc",
                    coverUrl = "cover",
                    category = "都市",
                    episodeCount = 12,
                    tags = listOf("逆袭"),
                    rating = 8.2,
                    createdAt = "2026-07-25T00:00:00Z",
                    updatedAt = "2026-07-25T00:00:00Z",
                    heat = 12345,
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 20, total = 1, totalPages = 1),
        )
        coEvery { apiService.getDramaChannel("all", 1, 20) } returns response

        val result = dataSource.getTheaterFeed(channel = "all", page = 1, pageSize = 20)

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.data.size)
    }
}
