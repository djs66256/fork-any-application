package com.djs66256.short_drama.domain.model

data class ToggleCommentLikeResult(
    val commentId: String,
    val liked: Boolean,
    val likeCount: Int,
)
