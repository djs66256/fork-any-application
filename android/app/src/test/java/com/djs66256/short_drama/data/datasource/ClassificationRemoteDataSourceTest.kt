package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.ClassificationDimensionDto
import com.djs66256.short_drama.data.dto.ClassificationTagsPayloadDto
import com.djs66256.short_drama.data.dto.ClassificationTagsResponseDto
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

class ClassificationRemoteDataSourceTest {
    private val apiService = mockk<ApiService>()
    private val dataSource = ClassificationRemoteDataSource(
        apiService = apiService,
        json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        },
    )

    @Test
    fun `T-07 getClassificationTags returns success for normal payload`() = runTest {
        val response = ClassificationTagsResponseDto(
            data = ClassificationTagsPayloadDto(
                gender = "female",
                dimensions = listOf(
                    ClassificationDimensionDto(
                        key = "era_background",
                        name = "时代背景",
                        tags = listOf("都市", "校园"),
                    ),
                    ClassificationDimensionDto(
                        key = "theme_plot",
                        name = "主题情节",
                        tags = emptyList(),
                    ),
                    ClassificationDimensionDto(
                        key = "character_setting",
                        name = "角色设定",
                        tags = listOf("萌宝"),
                    ),
                ),
            ),
        )
        coEvery { apiService.getDramaTags("female") } returns response

        val result = dataSource.getClassificationTags("female")

        assertTrue(result is ApiResult.Success)
        assertEquals("female", (result as ApiResult.Success).data.data.gender)
        assertEquals(3, result.data.data.dimensions.size)
        assertTrue(result.data.data.dimensions[1].tags.isEmpty())
    }

    @Test
    fun `T-07 classification error body is parsed into ApiResult Error`() = runTest {
        val body = """{"error":{"code":"VALIDATION_ERROR","message":"gender 非法"}}"""
            .toResponseBody("application/json".toMediaType())
        val exception = HttpException(Response.error<Any>(400, body))
        coEvery { apiService.getDramaTags("bad") } throws exception

        val result = dataSource.getClassificationTags("bad")

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("VALIDATION_ERROR", result.code)
        assertEquals("gender 非法", result.message)
    }

    @Test
    fun `T-07 unexpected exception becomes ApiResult Exception`() = runTest {
        val throwable = IllegalStateException("boom")
        coEvery { apiService.getDramaTags("all") } throws throwable

        val result = dataSource.getClassificationTags("all")

        assertTrue(result is ApiResult.Exception)
        assertEquals(throwable, (result as ApiResult.Exception).throwable)
    }
}
