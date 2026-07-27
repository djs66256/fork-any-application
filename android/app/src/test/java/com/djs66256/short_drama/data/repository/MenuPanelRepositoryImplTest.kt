package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.MenuPanelRemoteDataSource
import com.djs66256.short_drama.data.dto.RecentlyViewedDataDto
import com.djs66256.short_drama.data.dto.RecentlyViewedItemDto
import com.djs66256.short_drama.data.dto.RecentlyViewedResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuPanelRepositoryImplTest {

    private val remoteDataSource = mockk<MenuPanelRemoteDataSource>()
    private val repository = MenuPanelRepositoryImpl(remoteDataSource)

    @Test
    fun `T-04 repository maps dto list into domain recently viewed items`() = runTest {
        coEvery {
            remoteDataSource.getRecentlyViewed(playbackSessionId = "session-123")
        } returns ApiResult.Success(
            RecentlyViewedResponseDto(
                data = RecentlyViewedDataDto(
                    items = listOf(
                        RecentlyViewedItemDto(
                            dramaId = "drama-1",
                            title = "最近在看 1",
                            coverUrl = null,
                            episodeNumber = 12,
                            progress = 20.0,
                            updatedAt = "2026-07-27T15:20:00.000Z",
                        ),
                        RecentlyViewedItemDto(
                            dramaId = "drama-2",
                            title = "最近在看 2",
                            coverUrl = "https://example.com/2.jpg",
                            episodeNumber = 3,
                            progress = 8.0,
                            updatedAt = "2026-07-27T15:21:00.000Z",
                        ),
                    ),
                ),
            ),
        )

        val result = repository.getRecentlyViewed("session-123")

        assertTrue(result is ApiResult.Success)
        val items = (result as ApiResult.Success).data
        assertEquals(listOf("drama-1", "drama-2"), items.map { it.dramaId })
        assertEquals(null, items.first().coverUrl)
        assertEquals(3, items.last().episodeNumber)
    }

    @Test
    fun `T-04 repository keeps empty list as empty state`() = runTest {
        coEvery {
            remoteDataSource.getRecentlyViewed(playbackSessionId = "session-123")
        } returns ApiResult.Success(
            RecentlyViewedResponseDto(
                data = RecentlyViewedDataDto(items = emptyList()),
            ),
        )

        val result = repository.getRecentlyViewed("session-123")

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun `T-04 repository forwards api errors without mixing player write responsibilities`() = runTest {
        coEvery {
            remoteDataSource.getRecentlyViewed(playbackSessionId = "session-123")
        } returns ApiResult.Error(code = "INTERNAL_ERROR", message = "服务异常")

        val result = repository.getRecentlyViewed("session-123")

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("INTERNAL_ERROR", result.code)
        assertEquals("服务异常", result.message)
    }
}
