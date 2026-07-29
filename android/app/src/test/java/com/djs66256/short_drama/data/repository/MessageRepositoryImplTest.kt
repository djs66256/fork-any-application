package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.MessageRemoteDataSource
import com.djs66256.short_drama.data.dto.InteractionMessageDto
import com.djs66256.short_drama.data.dto.InteractionMessageListResponseDto
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.dto.SystemMessageDto
import com.djs66256.short_drama.data.dto.SystemMessageListResponseDto
import com.djs66256.short_drama.domain.model.InteractionMessageType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageRepositoryImplTest {

    private val remoteDataSource = mockk<MessageRemoteDataSource>()
    private val repository = MessageRepositoryImpl(remoteDataSource)

    @Test
    fun `T-03 repository maps preview null as empty state`() = runTest {
        coEvery { remoteDataSource.getMessagePreview() } returns ApiResult.Success(null)

        val result = repository.getMessagePreview()

        assertTrue(result is ApiResult.Success)
        assertNull((result as ApiResult.Success).data)
        coVerify { remoteDataSource.getMessagePreview() }
    }

    @Test
    fun `T-03 repository maps system and interaction pages`() = runTest {
        coEvery { remoteDataSource.getSystemMessages(page = 1, pageSize = 20) } returns ApiResult.Success(
            SystemMessageListResponseDto(
                data = listOf(
                    SystemMessageDto(
                        id = "system-1",
                        title = "系统通知",
                        summary = "摘要",
                        sentAt = "2026-07-29T08:00:00.000Z",
                    ),
                ),
                pagination = PaginationDto(page = 1, pageSize = 20, total = 1, totalPages = 1),
            ),
        )
        coEvery {
            remoteDataSource.getInteractionMessages(page = 1, pageSize = 20)
        } returns ApiResult.Success(
            InteractionMessageListResponseDto(
                data = listOf(
                    InteractionMessageDto(
                        id = "interaction-1",
                        type = "comment_like",
                        title = "有人点赞了你的评论",
                        summary = "收到一个新赞。",
                        sentAt = "2026-07-29T09:00:00.000Z",
                    ),
                ),
                pagination = PaginationDto(page = 1, pageSize = 20, total = 1, totalPages = 1),
            ),
        )

        val systemResult = repository.getSystemMessages(page = 1, pageSize = 20)
        val interactionResult = repository.getInteractionMessages(page = 1, pageSize = 20)

        assertTrue(systemResult is ApiResult.Success)
        assertEquals(1, (systemResult as ApiResult.Success).data.items.size)
        assertTrue(interactionResult is ApiResult.Success)
        assertEquals(
            InteractionMessageType.COMMENT_LIKE,
            (interactionResult as ApiResult.Success).data.items.single().type,
        )
    }

    @Test
    fun `T-03 repository forwards preview error semantics`() = runTest {
        coEvery { remoteDataSource.getMessagePreview() } returns ApiResult.Error(
            code = "SERVICE_UNAVAILABLE",
            message = "服务暂不可用",
        )

        val result = repository.getMessagePreview()

        assertTrue(result is ApiResult.Error)
        assertEquals("SERVICE_UNAVAILABLE", (result as ApiResult.Error).code)
    }
}
