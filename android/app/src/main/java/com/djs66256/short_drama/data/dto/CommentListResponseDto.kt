package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.CommentPage
import kotlinx.serialization.Serializable

@Serializable
data class CommentListResponseDto(
    val data: List<CommentDto>,
    val pagination: PaginationDto,
) {
    fun toDomain(dramaId: String): CommentPage {
        return CommentPage(
            dramaId = dramaId,
            items = data.map(CommentDto::toDomain),
            page = pagination.page,
            pageSize = pagination.pageSize,
            total = pagination.total,
            totalPages = pagination.totalPages,
        )
    }
}
