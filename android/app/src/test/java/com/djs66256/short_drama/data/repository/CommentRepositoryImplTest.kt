package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.CommentRemoteDataSource
import com.djs66256.short_drama.data.dto.CommentDto
import com.djs66256.short_drama.data.dto.CommentListResponseDto
import com.djs66256.short_drama.data.dto.CommentUserDto
import com.djs66256.short_drama.data.dto.CreateCommentRequestDto
import com.djs66256.short_drama.data.dto.PaginationDto
import com.djs66256.short_drama.data.dto.ToggleCommentLikeResponseDto
import com.djs66256.short_drama.domain.model.CommentQuery
import com.djs66256.short_drama.domain.model.CommentSort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentRepositoryImplTest {

    private val remoteDataSource = mockk<CommentRemoteDataSource>()
    private val repository = CommentRepositoryImpl(remoteDataSource)

    @Test
    fun `T-01 getComments forwards latest query and maps snake case payload`() = runTest {
        val query = CommentQuery(dramaId = "drama-1", page = 1, pageSize = 20, sort = CommentSort.LATEST)
        coEvery {
            remoteDataSource.getComments(
                dramaId = "drama-1",
                page = 1,
                pageSize = 20,
                sort = "latest",
            )
        } returns ApiResult.Success(
            CommentListResponseDto(
                data = listOf(sampleCommentDto(id = "comment-1", liked = false, likeCount = 12)),
                pagination = PaginationDto(page = 1, pageSize = 20, total = 36, totalPages = 2),
            ),
        )

        val result = repository.getComments(query)

        assertTrue(result is ApiResult.Success)
        val page = (result as ApiResult.Success).data
        assertEquals("drama-1", page.dramaId)
        assertEquals(1, page.items.size)
        assertEquals("comment-1", page.items.single().id)
        assertEquals("评论正文", page.items.single().content)
        assertEquals(12, page.items.single().likeCount)
        assertEquals(false, page.items.single().liked)
        assertEquals("用户昵称", page.items.single().user.displayName)
        assertEquals(1, page.page)
        assertEquals(20, page.pageSize)
        assertEquals(36, page.total)
        assertEquals(2, page.totalPages)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `T-01 getComments forwards hot sort without rewriting query`() = runTest {
        val query = CommentQuery(dramaId = "drama-1", page = 2, pageSize = 10, sort = CommentSort.HOT)
        coEvery {
            remoteDataSource.getComments(
                dramaId = "drama-1",
                page = 2,
                pageSize = 10,
                sort = "hot",
            )
        } returns ApiResult.Success(
            CommentListResponseDto(
                data = emptyList(),
                pagination = PaginationDto(page = 2, pageSize = 10, total = 10, totalPages = 2),
            ),
        )

        val result = repository.getComments(query)

        assertTrue(result is ApiResult.Success)
        coVerify(exactly = 1) {
            remoteDataSource.getComments(
                dramaId = "drama-1",
                page = 2,
                pageSize = 10,
                sort = "hot",
            )
        }
    }

    @Test
    fun `T-02 createComment trims content in request and maps comment payload`() = runTest {
        coEvery {
            remoteDataSource.createComment(
                dramaId = "drama-1",
                request = CreateCommentRequestDto(content = "新的评论"),
            )
        } returns ApiResult.Success(
            sampleCommentDto(
                id = "comment-new",
                liked = false,
                likeCount = 0,
                content = "新的评论",
            ),
        )

        val result = repository.createComment(dramaId = "drama-1", content = "  新的评论  ")

        assertTrue(result is ApiResult.Success)
        val comment = (result as ApiResult.Success).data
        assertEquals("comment-new", comment.id)
        assertEquals("新的评论", comment.content)
        assertEquals(0, comment.likeCount)
        assertEquals(false, comment.liked)
        coVerify(exactly = 1) {
            remoteDataSource.createComment(
                dramaId = "drama-1",
                request = CreateCommentRequestDto(content = "新的评论"),
            )
        }
    }

    @Test
    fun `T-02 toggleLike maps local update result`() = runTest {
        coEvery {
            remoteDataSource.toggleLike(dramaId = "drama-1", commentId = "comment-1")
        } returns ApiResult.Success(
            ToggleCommentLikeResponseDto(
                commentId = "comment-1",
                liked = true,
                likeCount = 13,
            ),
        )

        val result = repository.toggleCommentLike(dramaId = "drama-1", commentId = "comment-1")

        assertTrue(result is ApiResult.Success)
        val likeResult = (result as ApiResult.Success).data
        assertEquals("comment-1", likeResult.commentId)
        assertEquals(true, likeResult.liked)
        assertEquals(13, likeResult.likeCount)
    }

    @Test
    fun `T-02 repository keeps ApiResult error and exception semantics`() = runTest {
        val error = ApiResult.Error(code = "SERVICE_UNAVAILABLE", message = "服务暂不可用")
        val exception = ApiResult.Exception(IllegalStateException("network"))
        coEvery {
            remoteDataSource.getComments(dramaId = "drama-1", page = 1, pageSize = 20, sort = "latest")
        } returns error
        coEvery {
            remoteDataSource.createComment(dramaId = "drama-1", request = CreateCommentRequestDto("评论"))
        } returns exception

        val listResult = repository.getComments(CommentQuery(dramaId = "drama-1"))
        val createResult = repository.createComment(dramaId = "drama-1", content = "评论")

        assertTrue(listResult is ApiResult.Error)
        assertEquals("SERVICE_UNAVAILABLE", (listResult as ApiResult.Error).code)
        assertTrue(createResult is ApiResult.Exception)
        assertEquals("network", (createResult as ApiResult.Exception).throwable.message)
    }

    private fun sampleCommentDto(
        id: String,
        liked: Boolean,
        likeCount: Int,
        content: String = "评论正文",
    ): CommentDto {
        return CommentDto(
            id = id,
            dramaId = "drama-1",
            content = content,
            likeCount = likeCount,
            liked = liked,
            createdAt = "2026-07-29T09:30:00.000Z",
            updatedAt = "2026-07-29T09:30:00.000Z",
            user = CommentUserDto(
                id = "user-1",
                displayName = "用户昵称",
                avatarUrl = null,
            ),
        )
    }
}
