package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.RecentlyViewed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentlyViewedResponseDto(
    val code: Int = 0,
    val data: RecentlyViewedDataDto,
    val message: String = "ok",
)

@Serializable
data class RecentlyViewedDataDto(
    val items: List<RecentlyViewedItemDto> = emptyList(),
)

@Serializable
data class RecentlyViewedItemDto(
    @SerialName("drama_id")
    val dramaId: String,
    val title: String,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    @SerialName("episode_number")
    val episodeNumber: Int,
    val progress: Double,
    @SerialName("updated_at")
    val updatedAt: String,
) {
    fun toDomain(): RecentlyViewed = RecentlyViewed(
        dramaId = dramaId,
        title = title,
        coverUrl = coverUrl,
        episodeNumber = episodeNumber,
        progress = progress,
        updatedAt = updatedAt,
    )
}
