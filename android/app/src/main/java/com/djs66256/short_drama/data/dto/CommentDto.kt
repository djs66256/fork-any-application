package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.Comment
import com.djs66256.short_drama.domain.model.CommentUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentUserDto(
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
) {
    fun toDomain(): CommentUser {
        return CommentUser(
            id = id,
            displayName = displayName,
            avatarUrl = avatarUrl,
        )
    }
}

@Serializable
data class CommentDto(
    val id: String,
    @SerialName("drama_id")
    val dramaId: String,
    val content: String,
    @SerialName("like_count")
    val likeCount: Int,
    val liked: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val user: CommentUserDto,
) {
    fun toDomain(): Comment {
        return Comment(
            id = id,
            dramaId = dramaId,
            content = content,
            likeCount = likeCount,
            liked = liked,
            createdAt = createdAt,
            updatedAt = updatedAt,
            user = user.toDomain(),
        )
    }
}
