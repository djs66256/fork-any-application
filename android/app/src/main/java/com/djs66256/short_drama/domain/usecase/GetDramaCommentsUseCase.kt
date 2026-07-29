package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.CommentPage
import com.djs66256.short_drama.domain.model.CommentQuery
import com.djs66256.short_drama.domain.repository.CommentRepository
import javax.inject.Inject

class GetDramaCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository,
) {
    suspend operator fun invoke(query: CommentQuery): ApiResult<CommentPage> {
        return commentRepository.getComments(query)
    }
}
