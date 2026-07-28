package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.TheaterChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TheaterFeedResponseDtoTest {

    @Test
    fun `T-01 theater feed dto maps fields and next page state`() {
        val response = TheaterFeedResponseDto(
            data = listOf(
                TheaterDramaDto(
                    id = "drama-1",
                    title = "逆袭归来后我成了豪门团宠",
                    description = "落魄千金重回豪门，在误会与守护中逆风翻盘。",
                    coverUrl = "https://example.com/dramas/001.jpg",
                    category = "都市",
                    episodeCount = 68,
                    tags = listOf("逆袭", "豪门"),
                    rating = 8.9,
                    createdAt = "2026-07-25T00:00:00Z",
                    updatedAt = "2026-07-25T00:00:00Z",
                    heat = 98210,
                ),
            ),
            pagination = PaginationDto(
                page = 1,
                pageSize = 20,
                total = 25,
                totalPages = 2,
            ),
        )

        val page = response.toDomain(channel = TheaterChannel.ALL)

        assertEquals(TheaterChannel.ALL, page.channel)
        assertEquals(1, page.items.size)
        assertEquals("drama-1", page.items.single().id)
        assertEquals("https://example.com/dramas/001.jpg", page.items.single().coverUrl)
        assertEquals(98210, page.items.single().heat)
        assertEquals(1, page.page)
        assertEquals(20, page.pageSize)
        assertEquals(25, page.total)
        assertEquals(2, page.totalPages)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `T-01 theater feed dto keeps zero heat and stops at last page`() {
        val response = TheaterFeedResponseDto(
            data = listOf(
                TheaterDramaDto(
                    id = "drama-2",
                    title = "空山新雨",
                    description = "简介",
                    coverUrl = null,
                    category = "校园",
                    episodeCount = 24,
                    tags = emptyList(),
                    rating = 0.0,
                    createdAt = "2026-07-20T00:00:00Z",
                    updatedAt = "2026-07-21T00:00:00Z",
                    heat = 0,
                ),
            ),
            pagination = PaginationDto(
                page = 1,
                pageSize = 20,
                total = 1,
                totalPages = 1,
            ),
        )

        val page = response.toDomain(channel = TheaterChannel.REAL)

        assertEquals(TheaterChannel.REAL, page.channel)
        assertEquals(0, page.items.single().heat)
        assertEquals("", page.items.single().coverUrl)
        assertFalse(page.hasNextPage)
    }
}
