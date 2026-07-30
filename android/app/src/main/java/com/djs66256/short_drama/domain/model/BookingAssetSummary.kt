package com.djs66256.short_drama.domain.model

data class BookingAssetSummary(
    val onlineCount: Int = 0,
    val upcomingCount: Int = 0,
) {
    fun countFor(status: BookingAssetStatus): Int = when (status) {
        BookingAssetStatus.ONLINE -> onlineCount
        BookingAssetStatus.UPCOMING -> upcomingCount
    }
}
