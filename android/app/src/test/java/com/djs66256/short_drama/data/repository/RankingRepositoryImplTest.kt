package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.RankingRemoteDataSource
import com.djs66256.short_drama.data.dto.BookDramaResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.dto.RankingDramaDto
import com.djs66256.short_drama.data.dto.RankingListResponseDto
import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingQuery
import com.djs66256.short_drama.domain.model.RankingType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingRepositoryImplTest {

    private val remoteDataSource = mockk<RankingRemoteDataSource>()
    private val repository = RankingRepositoryImpl(remoteDataSource)

    @Test
    fun `T-10 getDramaRankings maps dto fields and pagination`() = runTest {
        val query = RankingQuery(
            contentType = RankingContentType.ALL,
            type = RankingType.HOT,
            page = 1,
            pageSize = 10,
        )
        val response = RankingListResponseDto(
            data = listOf(
                RankingDramaDto(
                    id = "drama-1",
                    title = "示例短剧",
                    description = "排行卡片描述",
                    coverUrl = null,
                    category = "都市",
                    episodeCount = 12,
                    tags = listOf("逆袭", "甜宠"),
                    rating = 8.6,
                    createdAt = "2026-07-25T00:00:00Z",
                    updatedAt = "2026-07-25T00:00:00Z",
                    contentType = "live_action",
                    playCount = 1200,
                    bookingCount = 88,
                    recommendationScore = 98.5,
                    isBooked = false,
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 10, total = 12, totalPages = 2),
        )
        coEvery {
            remoteDataSource.getDramaRankings(
                type = "hot",
                contentType = "all",
                page = 1,
                pageSize = 10,
            )
        } returns ApiResult.Success(response)

        val result = repository.getDramaRankings(query)

        assertTrue(result is ApiResult.Success)
        val page = (result as ApiResult.Success).data
        assertEquals(1, page.items.size)
        assertEquals("drama-1", page.items.single().id)
        assertEquals("", page.items.single().coverUrl)
        assertEquals(RankingContentType.LIVE_ACTION, page.items.single().contentType)
        assertEquals(1200, page.items.single().playCount)
        assertEquals(88, page.items.single().bookingCount)
        assertEquals(98.5, page.items.single().recommendationScore, 0.0)
        assertEquals(1, page.page)
        assertEquals(10, page.pageSize)
        assertEquals(12, page.total)
        assertEquals(2, page.totalPages)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `T-08 bookDrama maps response to domain result`() = runTest {
        coEvery { remoteDataSource.bookDrama("drama-1") } returns ApiResult.Success(
            BookDramaResponseDto(
                dramaId = "drama-1",
                booked = true,
                bookingCount = 89,
            ),
        )

        val result = repository.bookDrama("drama-1")

        assertTrue(result is ApiResult.Success)
        val bookResult = (result as ApiResult.Success).data
        assertEquals("drama-1", bookResult.dramaId)
        assertTrue(bookResult.booked)
        assertEquals(89, bookResult.bookingCount)
    }
}
