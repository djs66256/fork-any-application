package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.RecentlyViewedDataDto
import com.djs66256.short_drama.data.dto.RecentlyViewedItemDto
import com.djs66256.short_drama.data.dto.RecentlyViewedResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
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

class MenuPanelRemoteDataSourceTest {

    private val apiService = mockk<ApiService>()
    private val json = Json { ignoreUnknownKeys = true }
    private val dataSource = MenuPanelRemoteDataSource(apiService, json)

    @Test
    fun `T-04 recently viewed forwards playback session header and keeps nullable cover`() = runTest {
        val sessionId = "session-123"
        coEvery {
            apiService.getRecentlyViewed(playbackSessionId = sessionId)
        } returns RecentlyViewedResponseDto(
            data = RecentlyViewedDataDto(
                items = listOf(
                    RecentlyViewedItemDto(
                        dramaId = "drama-1",
                        title = "最近在看",
                        coverUrl = null,
                        episodeNumber = 12,
                        progress = 128.5,
                        updatedAt = "2026-07-27T15:20:00.000Z",
                    ),
                ),
            ),
        )

        val result = dataSource.getRecentlyViewed(sessionId)

        assertTrue(result is ApiResult.Success)
        val items = (result as ApiResult.Success).data.data.items
        assertEquals(1, items.size)
        assertEquals(null, items.single().coverUrl)
        coVerify(exactly = 1) { apiService.getRecentlyViewed(playbackSessionId = sessionId) }
    }

    @Test
    fun `T-04 recently viewed maps structured http errors`() = runTest {
        val sessionId = "session-123"
        val errorBody = """
            {
              "error": {
                "code": "INVALID_PLAYBACK_SESSION",
                "message": "session 无效"
              }
            }
        """.trimIndent()
        coEvery {
            apiService.getRecentlyViewed(playbackSessionId = sessionId)
        } throws httpException(statusCode = 400, body = errorBody)

        val result = dataSource.getRecentlyViewed(sessionId)

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("INVALID_PLAYBACK_SESSION", result.code)
        assertEquals("session 无效", result.message)
    }

    private fun httpException(statusCode: Int, body: String): HttpException {
        return HttpException(
            Response.error<Any>(
                statusCode,
                body.toResponseBody("application/json".toMediaType()),
            ),
        )
    }
}
