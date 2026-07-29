package com.djs66256.short_drama.domain.model

data class CommentUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
)

data class Comment(
    val id: String,
    val dramaId: String,
    val content: String,
    val likeCount: Int,
    val liked: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val user: CommentUser,
)
