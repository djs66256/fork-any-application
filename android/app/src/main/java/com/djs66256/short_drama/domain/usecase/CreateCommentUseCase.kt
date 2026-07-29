package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.Comment
import com.djs66256.short_drama.domain.repository.CommentRepository
import javax.inject.Inject

class CreateCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
) {
    suspend operator fun invoke(
        dramaId: String,
        content: String,
    ): ApiResult<Comment> {
        return commentRepository.createComment(dramaId, content)
    }
}
