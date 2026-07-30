package com.djs66256.short_drama.domain.model

private const val DEFAULT_BOOKING_PAGE = 1
private const val DEFAULT_BOOKING_PAGE_SIZE = 20

data class BookingAssetsQuery(
    val status: BookingAssetStatus = BookingAssetStatus.ONLINE,
    val page: Int = DEFAULT_BOOKING_PAGE,
    val pageSize: Int = DEFAULT_BOOKING_PAGE_SIZE,
)
