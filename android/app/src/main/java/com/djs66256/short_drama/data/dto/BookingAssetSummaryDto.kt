package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.BookingAssetSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookingAssetSummaryDto(
    @SerialName("online_count")
    val onlineCount: Int,
    @SerialName("upcoming_count")
    val upcomingCount: Int,
) {
    fun toDomain(): BookingAssetSummary = BookingAssetSummary(
        onlineCount = onlineCount,
        upcomingCount = upcomingCount,
    )
}
