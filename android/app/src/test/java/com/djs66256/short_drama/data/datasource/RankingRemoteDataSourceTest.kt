package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.BookDramaResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.dto.RankingDramaDto
import com.djs66256.short_drama.data.dto.RankingListResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RankingRemoteDataSourceTest {
    private val apiService = mockk<ApiService>()
    private val dataSource = RankingRemoteDataSource(
        apiService = apiService,
        json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        },
    )

    @Test
    fun `T-06 getDramaRankings returns success for normal payload`() = runTest {
        val response = RankingListResponseDto(
            data = listOf(
                RankingDramaDto(
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
                    contentType = "ai",
                    playCount = 999,
                    bookingCount = 66,
                    recommendationScore = 77.7,
                    isBooked = false,
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 10, total = 1, totalPages = 1),
        )
        coEvery { apiService.getDramaRankings("hot", "all", 1, 10) } returns response

        val result = dataSource.getDramaRankings("hot", "all", 1, 10)

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.data.size)
    }

    @Test
    fun `T-06 ranking error body is parsed into ApiResult Error`() = runTest {
        val body = """{"error":{"code":"VALIDATION_ERROR","message":"参数非法"}}"""
            .toResponseBody("application/json".toMediaType())
        val exception = HttpException(Response.error<Any>(400, body))
        coEvery { apiService.getDramaRankings("bad", "all", 1, 10) } throws exception

        val result = dataSource.getDramaRankings("bad", "all", 1, 10)

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("VALIDATION_ERROR", result.code)
        assertEquals("参数非法", result.message)
    }

    @Test
    fun `T-08 bookDrama returns success for normal payload`() = runTest {
        coEvery { apiService.bookDrama("drama-1") } returns BookDramaResponseDto(
            dramaId = "drama-1",
            booked = true,
            bookingCount = 88,
        )

        val result = dataSource.bookDrama("drama-1")

        assertTrue(result is ApiResult.Success)
        assertEquals("drama-1", (result as ApiResult.Success).data.dramaId)
    }
}
