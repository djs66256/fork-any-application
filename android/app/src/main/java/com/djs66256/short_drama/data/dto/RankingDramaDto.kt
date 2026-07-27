package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingDrama
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RankingDramaDto(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    val category: String,
    @SerialName("episode_count")
    val episodeCount: Int,
    val tags: List<String> = emptyList(),
    val rating: Double = 0.0,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("play_count")
    val playCount: Int,
    @SerialName("booking_count")
    val bookingCount: Int,
    @SerialName("recommendation_score")
    val recommendationScore: Double,
    @SerialName("is_booked")
    val isBooked: Boolean = false,
) {
    fun toDomain(): RankingDrama = RankingDrama(
        id = id,
        title = title,
        description = description,
        coverUrl = coverUrl.orEmpty(),
        category = category,
        episodeCount = episodeCount,
        tags = tags,
        rating = rating,
        createdAt = createdAt,
        updatedAt = updatedAt,
        contentType = RankingContentType.fromApiValue(contentType),
        playCount = playCount,
        bookingCount = bookingCount,
        recommendationScore = recommendationScore,
        isBooked = isBooked,
    )
}
