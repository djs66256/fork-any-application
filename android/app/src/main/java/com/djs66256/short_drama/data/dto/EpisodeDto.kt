package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.Episode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeDto(
    val id: String,
    @SerialName("drama_id")
    val dramaId: String,
    val title: String,
    @SerialName("episode_number")
    val episodeNumber: Int,
    @SerialName("video_url")
    val videoUrl: String,
    val duration: Int,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String = "",
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
) {
    fun toDomain(): Episode = Episode(
        id = id,
        dramaId = dramaId,
        title = title,
        episodeNumber = episodeNumber,
        videoUrl = videoUrl,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
