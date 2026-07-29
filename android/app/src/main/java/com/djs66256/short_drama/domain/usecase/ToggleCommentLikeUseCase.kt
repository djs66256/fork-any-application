package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.ToggleCommentLikeResult
import com.djs66256.short_drama.domain.repository.CommentRepository
import javax.inject.Inject

class ToggleCommentLikeUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
) {
    suspend operator fun invoke(
        dramaId: String,
        commentId: String,
    ): ApiResult<ToggleCommentLikeResult> {
        return commentRepository.toggleCommentLike(dramaId, commentId)
    }
}
