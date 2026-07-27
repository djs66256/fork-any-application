package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.BookDramaResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookDramaResponseDto(
    @SerialName("drama_id")
    val dramaId: String,
    val booked: Boolean,
    @SerialName("booking_count")
    val bookingCount: Int,
) {
    fun toDomain(): BookDramaResult = BookDramaResult(
        dramaId = dramaId,
        booked = booked,
        bookingCount = bookingCount,
    )
}
