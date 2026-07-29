package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.ToggleCommentLikeResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToggleCommentLikeResponseDto(
    @SerialName("comment_id")
    val commentId: String,
    val liked: Boolean,
    @SerialName("like_count")
    val likeCount: Int,
) {
    fun toDomain(): ToggleCommentLikeResult {
        return ToggleCommentLikeResult(
            commentId = commentId,
            liked = liked,
            likeCount = likeCount,
        )
    }
}
