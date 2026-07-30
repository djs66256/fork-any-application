package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.BookingAssetDto
import com.djs66256.short_drama.data.dto.BookingAssetSummaryDto
import com.djs66256.short_drama.data.dto.BookingAssetsResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.dto.TheaterDramaDto
import com.djs66256.short_drama.data.dto.TheaterFeedResponseDto
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

class DramaRemoteDataSourceTest {

    private val apiService = mockk<ApiService>()
    private val dataSource = DramaRemoteDataSource(
        apiService = apiService,
        json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        },
    )

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

    @Test
    fun `T-11 getUserBookings returns success for normal payload`() = runTest {
        val response = BookingAssetsResponseDto(
            data = listOf(
                BookingAssetDto(
                    dramaId = "drama-1",
                    title = "我的预约短剧",
                    coverUrl = "https://example.com/cover.jpg",
                    episodeCount = 24,
                    bookedAt = "2026-07-30T03:25:00.000Z",
                    availabilityStatus = "online",
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 20, total = 1, totalPages = 1),
            summary = BookingAssetSummaryDto(onlineCount = 1, upcomingCount = 2),
        )
        coEvery { apiService.getUserBookings("online", 1, 20) } returns response

        val result = dataSource.getUserBookings(status = "online", page = 1, pageSize = 20)

        assertTrue(result is ApiResult.Success)
        result as ApiResult.Success
        assertEquals(1, result.data.data.size)
        assertEquals(2, result.data.summary.upcomingCount)
    }

    @Test
    fun `T-11 getUserBookings parses error body into ApiResult Error`() = runTest {
        val body = """{"error":{"code":"AUTH_UNAUTHORIZED","message":"请先登录"}}"""
            .toResponseBody("application/json".toMediaType())
        val exception = HttpException(Response.error<Any>(401, body))
        coEvery { apiService.getUserBookings("online", 1, 20) } throws exception

        val result = dataSource.getUserBookings(status = "online", page = 1, pageSize = 20)

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("AUTH_UNAUTHORIZED", result.code)
        assertEquals("请先登录", result.message)
    }
}
