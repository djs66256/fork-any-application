package com.djs66256.short_drama.domain.model

enum class BookingAssetStatus(
    val apiValue: String,
    val label: String,
) {
    ONLINE(apiValue = "online", label = "已上线"),
    UPCOMING(apiValue = "upcoming", label = "待上线"),
    ;

    companion object {
        fun fromApiValue(value: String): BookingAssetStatus? {
            return entries.firstOrNull { it.apiValue == value.trim() }
        }
    }
}
