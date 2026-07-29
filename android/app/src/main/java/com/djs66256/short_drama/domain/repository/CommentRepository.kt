package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Comment
import com.djs66256.short_drama.domain.model.CommentPage
import com.djs66256.short_drama.domain.model.CommentQuery
import com.djs66256.short_drama.domain.model.ToggleCommentLikeResult

interface CommentRepository {
    suspend fun getComments(query: CommentQuery): ApiResult<CommentPage>

    suspend fun createComment(dramaId: String, content: String): ApiResult<Comment>

    suspend fun toggleCommentLike(dramaId: String, commentId: String): ApiResult<ToggleCommentLikeResult>
}
