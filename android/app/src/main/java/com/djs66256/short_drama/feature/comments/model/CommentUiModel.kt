package com.djs66256.short_drama.feature.comments.model

import com.djs66256.short_drama.domain.model.Comment

data class CommentUiModel(
    val id: String,
    val userDisplayName: String,
    val userAvatarUrl: String?,
    val content: String,
    val likeCount: Int,
    val liked: Boolean,
    val createdAt: String,
)

fun Comment.toUiModel(): CommentUiModel {
    return CommentUiModel(
        id = id,
        userDisplayName = user.displayName,
        userAvatarUrl = user.avatarUrl,
        content = content,
        likeCount = likeCount,
        liked = liked,
        createdAt = createdAt,
    )
}
