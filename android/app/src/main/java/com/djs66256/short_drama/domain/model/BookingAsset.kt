package com.djs66256.short_drama.domain.model

data class BookingAsset(
    val dramaId: String,
    val title: String,
    val coverUrl: String,
    val episodeCount: Int,
    val bookedAt: String,
    val availabilityStatus: BookingAssetStatus,
)
