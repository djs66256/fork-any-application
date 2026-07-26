package com.djs66256.short_drama.domain.model

/**
 * Domain entity representing an episode of a short drama.
 * Fields align with the Backend Zod Schema.
 */
data class Episode(
    val id: String,
    val dramaId: String,
    val title: String,
    val episodeNumber: Int,
    val videoUrl: String,
    val duration: Int,
    val thumbnailUrl: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
)
