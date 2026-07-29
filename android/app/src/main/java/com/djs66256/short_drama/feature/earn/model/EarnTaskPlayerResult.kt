package com.djs66256.short_drama.feature.earn.model

enum class EarnTaskPlayerResultReason(val wireValue: String) {
    PLAYBACK_ENDED("playback-ended"),
    USER_EXIT("user-exit"),
    BACKGROUNDED("backgrounded"),
    ERROR("error"),
    CONTAINER_RECREATED("container-recreated"),
}

data class EarnTaskPlayerResult(
    val taskId: String,
    val videoId: String,
    val completed: Boolean,
    val reason: EarnTaskPlayerResultReason,
    val source: String = EARN_SOURCE,
) {
    fun isValid(): Boolean {
        return source == EARN_SOURCE && taskId.isNotBlank() && videoId.isNotBlank()
    }
}
