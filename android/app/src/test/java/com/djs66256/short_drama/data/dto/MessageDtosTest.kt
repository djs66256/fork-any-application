package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.InteractionMessageType
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageDtosTest {

    @Test
    fun `T-01 message preview dto maps to domain`() {
        val dto = MessagePreviewDto(
            title = "系统通知",
            summary = "你关注的剧集已更新第 12 集。",
            relativeTime = "2小时前",
        )

        val domain = dto.toDomain()

        assertEquals("系统通知", domain.title)
        assertEquals("你关注的剧集已更新第 12 集。", domain.summary)
        assertEquals("2小时前", domain.relativeTime)
    }

    @Test
    fun `T-01 message list dtos keep pagination and map types`() {
        val systemResponse = SystemMessageListResponseDto(
            data = listOf(
                SystemMessageDto(
                    id = "system-1",
                    title = "系统通知",
                    summary = "摘要",
                    sentAt = "2026-07-29T08:00:00.000Z",
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 20, total = 1, totalPages = 1),
        )
        val interactionResponse = InteractionMessageListResponseDto(
            data = listOf(
                InteractionMessageDto(
                    id = "interaction-1",
                    type = "comment_reply",
                    title = "有人回复了你的评论",
                    summary = "收到一条新回复。",
                    sentAt = "2026-07-29T09:00:00.000Z",
                ),
            ),
            pagination = PaginationDto(page = 1, pageSize = 20, total = 1, totalPages = 1),
        )

        val systemDomain = systemResponse.toDomain()
        val interactionDomain = interactionResponse.toDomain()

        assertEquals(1, systemDomain.items.size)
        assertEquals(1, systemDomain.page)
        assertEquals(20, systemDomain.pageSize)
        assertEquals(1, interactionDomain.items.size)
        assertEquals(InteractionMessageType.COMMENT_REPLY, interactionDomain.items.single().type)
    }
}
