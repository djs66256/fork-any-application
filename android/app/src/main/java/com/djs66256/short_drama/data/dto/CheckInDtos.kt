package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.CheckInDay
import com.djs66256.short_drama.domain.model.CheckInDayStatus
import com.djs66256.short_drama.domain.model.CheckInStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignInStatusDto(
    @SerialName("server_date")
    val serverDate: String,
    @SerialName("should_show_popup")
    val shouldShowPopup: Boolean,
    @SerialName("today_signed")
    val todaySigned: Boolean,
    @SerialName("current_streak")
    val currentStreak: Int,
    @SerialName("reward_copy")
    val rewardCopy: String,
    val days: List<SignInDayDto>,
)

@Serializable
data class SignInDayDto(
    val day: Int,
    val title: String,
    @SerialName("reward_label")
    val rewardLabel: String,
    val status: String,
)

fun SignInStatusDto.toDomain(): CheckInStatus {
    return CheckInStatus(
        serverDate = serverDate,
        shouldShowPopup = shouldShowPopup,
        todaySigned = todaySigned,
        currentStreak = currentStreak,
        rewardCopy = rewardCopy,
        days = days.map { it.toDomain() },
    )
}

fun SignInDayDto.toDomain(): CheckInDay {
    return CheckInDay(
        day = day,
        title = title,
        rewardLabel = rewardLabel,
        status = CheckInDayStatus.fromApiValue(status),
    )
}
