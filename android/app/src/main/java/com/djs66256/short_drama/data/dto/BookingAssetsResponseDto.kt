package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.BookingAssetsPage
import kotlinx.serialization.Serializable

@Serializable
data class BookingAssetsResponseDto(
    val data: List<BookingAssetDto>,
    val pagination: PaginationDto,
    val summary: BookingAssetSummaryDto,
) {
    fun toDomain(): BookingAssetsPage = BookingAssetsPage(
        items = data.map(BookingAssetDto::toDomain),
        page = pagination.page,
        pageSize = pagination.pageSize,
        total = pagination.total,
        totalPages = pagination.totalPages,
        summary = summary.toDomain(),
    )
}
