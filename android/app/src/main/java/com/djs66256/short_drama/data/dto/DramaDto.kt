package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.Drama
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DramaDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val category: String,
    @SerialName("episode_count")
    val episodeCount: Int,
    val tags: List<String> = emptyList(),
    val rating: Double? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
) {
    fun toDomain(): Drama = Drama(
        id = id,
        title = title,
        description = description,
        coverUrl = coverUrl.orEmpty(),
        category = category,
        episodeCount = episodeCount,
        tags = tags,
        rating = rating ?: 0.0,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
