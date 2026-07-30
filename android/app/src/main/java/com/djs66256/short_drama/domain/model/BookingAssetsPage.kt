package com.djs66256.short_drama.domain.model

data class BookingAssetsPage(
    val items: List<BookingAsset>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
    val summary: BookingAssetSummary,
) {
    val hasNextPage: Boolean = page < totalPages
}
