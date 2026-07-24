package com.djs66256.short_drama.domain.model

/**
 * Domain entity representing a short drama.
 * Fields align with the Backend Zod Schema.
 */
data class Drama(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val category: String,
    val episodeCount: Int,
    val tags: List<String>,
    val rating: Double,
    val createdAt: String,
    val updatedAt: String
)
