package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.DramaRemoteDataSource
import com.djs66256.short_drama.data.dto.DramaDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DramaRepositoryImplTest {

    private val remoteDataSource = mockk<DramaRemoteDataSource>()
    private val repository = DramaRepositoryImpl(remoteDataSource)

    @Test
    fun `T-07 getDramas maps dto fields required by home feed`() = runTest {
        val response = DramaListResponseDto(
            data = listOf(
                DramaDto(
                    id = "drama-1",
                    title = "示例短剧",
                    description = "首页卡片描述",
                    coverUrl = "https://example.com/cover.jpg",
                    category = "都市",
                    episodeCount = 12,
                    tags = listOf("逆袭", "甜宠"),
                    rating = 8.6,
                    createdAt = "2026-07-25T00:00:00Z",
                    updatedAt = "2026-07-25T00:00:00Z",
                ),
            ),
            pagination = PaginationDto(
                page = 1,
                pageSize = 10,
                total = 1,
                totalPages = 1,
            ),
        )
        coEvery { remoteDataSource.getDramas(1, 10) } returns ApiResult.Success(response)

        val result = repository.getDramas(page = 1, pageSize = 10)

        assertTrue(result is ApiResult.Success)
        val dramas = (result as ApiResult.Success).data
        assertEquals(1, dramas.size)
        assertEquals("drama-1", dramas.single().id)
        assertEquals("https://example.com/cover.jpg", dramas.single().coverUrl)
        assertEquals(12, dramas.single().episodeCount)
        assertEquals(listOf("逆袭", "甜宠"), dramas.single().tags)
        assertEquals(8.6, dramas.single().rating, 0.0)
    }
}
