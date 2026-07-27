package com.djs66256.short_drama.domain.model

data class RecentlyViewed(
    val dramaId: String,
    val title: String,
    val coverUrl: String?,
    val episodeNumber: Int,
    val progress: Double,
    val updatedAt: String,
)
