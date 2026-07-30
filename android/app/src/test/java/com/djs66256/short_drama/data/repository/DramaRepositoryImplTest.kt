package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.DramaRemoteDataSource
import com.djs66256.short_drama.data.dto.BookingAssetDto
import com.djs66256.short_drama.data.dto.BookingAssetSummaryDto
import com.djs66256.short_drama.data.dto.BookingAssetsResponseDto
import com.djs66256.short_drama.data.dto.DramaDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.dto.TheaterDramaDto
import com.djs66256.short_drama.data.dto.TheaterFeedResponseDto
import com.djs66256.short_drama.domain.model.BookingAssetStatus
import com.djs66256.short_drama.domain.model.BookingAssetsQuery
import com.djs66256.short_drama.domain.model.TheaterChannel
import com.djs66256.short_drama.domain.model.TheaterQuery
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
                DramaDto(
                    id = "drama-2",
                    title = "空封面短剧",
                    description = "首页卡片描述",
                    coverUrl = null,
                    category = "都市",
                    episodeCount = 20,
                    tags = listOf("系统"),
                    rating = null,
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
        assertEquals(2, dramas.size)
        assertEquals("drama-1", dramas[0].id)
        assertEquals("https://example.com/cover.jpg", dramas[0].coverUrl)
        assertEquals(12, dramas[0].episodeCount)
        assertEquals(listOf("逆袭", "甜宠"), dramas[0].tags)
        assertEquals(8.6, dramas[0].rating, 0.0)
        assertEquals("drama-2", dramas[1].id)
        assertEquals("", dramas[1].coverUrl)
        assertEquals(20, dramas[1].episodeCount)
        assertEquals(listOf("系统"), dramas[1].tags)
        assertEquals(0.0, dramas[1].rating, 0.0)
    }

    @Test
    fun `T-12 getTheaterFeed maps dto fields and pagination`() = runTest {
        val query = TheaterQuery(channel = TheaterChannel.ALL, page = 1, pageSize = 20)
        val response = TheaterFeedResponseDto(
            data = listOf(
                TheaterDramaDto(
                    id = "theater-1",
                    title = "剧场首屏短剧",
                    description = "剧场卡片描述",
                    coverUrl = null,
                    category = "都市",
                    episodeCount = 66,
                    tags = listOf("逆袭", "豪门"),
                    rating = 8.8,
                    createdAt = "2026-07-25T00:00:00Z",
                    updatedAt = "2026-07-25T00:00:00Z",
                    heat = 45678,
                ),
            ),
            pagination = PaginationDto(
                page = 1,
                pageSize = 20,
                total = 35,
                totalPages = 2,
            ),
        )
        coEvery { remoteDataSource.getTheaterFeed("all", 1, 20) } returns ApiResult.Success(response)

        val result = repository.getTheaterFeed(query)

        assertTrue(result is ApiResult.Success)
        val page = (result as ApiResult.Success).data
        assertEquals(TheaterChannel.ALL, page.channel)
        assertEquals(1, page.items.size)
        assertEquals("theater-1", page.items.single().id)
        assertEquals("", page.items.single().coverUrl)
        assertEquals(45678, page.items.single().heat)
        assertEquals(1, page.page)
        assertEquals(20, page.pageSize)
        assertEquals(35, page.total)
        assertEquals(2, page.totalPages)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `T-11 getBookingAssets maps dto items summary and pagination`() = runTest {
        val query = BookingAssetsQuery(status = BookingAssetStatus.UPCOMING, page = 1, pageSize = 20)
        val response = BookingAssetsResponseDto(
            data = listOf(
                BookingAssetDto(
                    dramaId = "drama-1",
                    title = "我的预约",
                    coverUrl = null,
                    episodeCount = 16,
                    bookedAt = "2026-07-30T03:25:00.000Z",
                    availabilityStatus = "upcoming",
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 20, total = 1, totalPages = 1),
            summary = BookingAssetSummaryDto(onlineCount = 3, upcomingCount = 1),
        )
        coEvery { remoteDataSource.getUserBookings("upcoming", 1, 20) } returns ApiResult.Success(response)

        val result = repository.getBookingAssets(query)

        assertTrue(result is ApiResult.Success)
        val page = (result as ApiResult.Success).data
        assertEquals(1, page.items.size)
        assertEquals("drama-1", page.items.single().dramaId)
        assertEquals("", page.items.single().coverUrl)
        assertEquals(BookingAssetStatus.UPCOMING, page.items.single().availabilityStatus)
        assertEquals(3, page.summary.onlineCount)
        assertEquals(1, page.summary.upcomingCount)
        assertEquals(1, page.page)
        assertEquals(20, page.pageSize)
        assertEquals(1, page.total)
        assertEquals(1, page.totalPages)
        assertTrue(!page.hasNextPage)
    }
}
