package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.DramaDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SearchRemoteDataSourceTest {
    private val apiService = mockk<ApiService>()
    private val dataSource = SearchRemoteDataSource(apiService)

    @Test
    fun `T-04 searchDramas returns success for normal payload`() = runTest {
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
        coEvery { apiService.searchDramas("逆袭", 1, 10) } returns response

        val result = dataSource.searchDramas("逆袭", 1, 10)

        assertTrue(result is ApiResult.Success)
        assertEquals(1, (result as ApiResult.Success).data.data.size)
    }

    @Test
    fun `T-04 error body is parsed into ApiResult Error`() = runTest {
        val body = """{"error":{"code":"VALIDATION_ERROR","message":"输入非法"}}"""
            .toResponseBody("application/json".toMediaType())
        val exception = HttpException(Response.error<Any>(400, body))
        coEvery { apiService.searchDramas("", 1, 10) } throws exception

        val result = dataSource.searchDramas("", 1, 10)

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("VALIDATION_ERROR", result.code)
        assertEquals("输入非法", result.message)
    }

    @Test
    fun `T-04 unexpected exception becomes ApiResult Exception`() = runTest {
        val throwable = IllegalStateException("boom")
        coEvery { apiService.getHotSearches() } throws throwable

        val result = dataSource.getHotSearches()

        assertTrue(result is ApiResult.Exception)
        assertEquals(throwable, (result as ApiResult.Exception).throwable)
    }
}
