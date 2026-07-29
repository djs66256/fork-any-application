package com.djs66256.short_drama.feature.earn.model

data class EarnTaskContext(
    val taskId: String,
    val source: String = EARN_SOURCE,
    val returnTarget: String = EARN_RETURN_TARGET,
    val videoId: String,
) {
    fun isValid(): Boolean {
        return taskId.isNotBlank() &&
            source == EARN_SOURCE &&
            returnTarget == EARN_RETURN_TARGET &&
            videoId.isNotBlank()
    }
}
