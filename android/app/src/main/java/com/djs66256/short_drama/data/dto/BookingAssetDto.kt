package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.BookingAsset
import com.djs66256.short_drama.domain.model.BookingAssetStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookingAssetDto(
    @SerialName("drama_id")
    val dramaId: String,
    val title: String,
    @SerialName("cover_url")
    val coverUrl: String? = null,
    @SerialName("episode_count")
    val episodeCount: Int,
    @SerialName("booked_at")
    val bookedAt: String,
    @SerialName("availability_status")
    val availabilityStatus: String,
) {
    fun toDomain(): BookingAsset {
        val status = requireNotNull(BookingAssetStatus.fromApiValue(availabilityStatus)) {
            "Unsupported booking asset status: $availabilityStatus"
        }
        return BookingAsset(
            dramaId = dramaId,
            title = title,
            coverUrl = coverUrl.orEmpty(),
            episodeCount = episodeCount,
            bookedAt = bookedAt,
            availabilityStatus = status,
        )
    }
}
