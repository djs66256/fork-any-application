package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.CommentRemoteDataSource
import com.djs66256.short_drama.data.dto.CreateCommentRequestDto
import com.djs66256.short_drama.domain.model.Comment
import com.djs66256.short_drama.domain.model.CommentPage
import com.djs66256.short_drama.domain.model.CommentQuery
import com.djs66256.short_drama.domain.model.ToggleCommentLikeResult
import com.djs66256.short_drama.domain.repository.CommentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepositoryImpl @Inject constructor(
    private val remoteDataSource: CommentRemoteDataSource,
) : CommentRepository {

    override suspend fun getComments(query: CommentQuery): ApiResult<CommentPage> {
        return when (
            val result = remoteDataSource.getComments(
                dramaId = query.dramaId,
                page = query.page,
                pageSize = query.pageSize,
                sort = query.sort.apiValue,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain(dramaId = query.dramaId))
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun createComment(dramaId: String, content: String): ApiResult<Comment> {
        return when (
            val result = remoteDataSource.createComment(
                dramaId = dramaId,
                request = CreateCommentRequestDto(content = content.trim()),
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun toggleCommentLike(
        dramaId: String,
        commentId: String,
    ): ApiResult<ToggleCommentLikeResult> {
        return when (val result = remoteDataSource.toggleLike(dramaId = dramaId, commentId = commentId)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }
}
