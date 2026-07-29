package com.djs66256.short_drama.domain.model

data class CheckInStatus(
    val serverDate: String,
    val shouldShowPopup: Boolean,
    val todaySigned: Boolean,
    val currentStreak: Int,
    val rewardCopy: String,
    val days: List<CheckInDay>,
)

data class CheckInDay(
    val day: Int,
    val title: String,
    val rewardLabel: String,
    val status: CheckInDayStatus,
)

enum class CheckInDayStatus {
    SIGNED,
    TODAY,
    LOCKED,
    ;

    companion object {
        fun fromApiValue(value: String): CheckInDayStatus = when (value.trim().lowercase()) {
            "signed" -> SIGNED
            "today" -> TODAY
            else -> LOCKED
        }
    }
}
