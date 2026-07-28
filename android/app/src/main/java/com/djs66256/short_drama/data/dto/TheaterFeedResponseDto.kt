package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.TheaterChannel
import com.djs66256.short_drama.domain.model.TheaterDrama
import com.djs66256.short_drama.domain.model.TheaterPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TheaterFeedResponseDto(
    val data: List<TheaterDramaDto>,
    val pagination: PaginationDto,
) {
    fun toDomain(channel: TheaterChannel): TheaterPage = TheaterPage(
        channel = channel,
        items = data.map(TheaterDramaDto::toDomain),
        page = pagination.page,
        pageSize = pagination.pageSize,
        total = pagination.total,
        totalPages = pagination.totalPages,
    )
}

@Serializable
data class TheaterDramaDto(
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
    val heat: Int,
) {
    fun toDomain(): TheaterDrama = TheaterDrama(
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
        heat = heat.coerceAtLeast(0),
    )
}
