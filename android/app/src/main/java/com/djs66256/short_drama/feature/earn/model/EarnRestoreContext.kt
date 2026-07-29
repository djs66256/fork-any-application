package com.djs66256.short_drama.feature.earn.model

enum class EarnRestoreReason(val wireValue: String) {
    LOGIN_RETURN("login-return"),
    TASK_RETURN("task-return"),
    CONTAINER_RECREATED("container-recreated"),
}

data class EarnRestoreContext(
    val source: String = EARN_SOURCE,
    val reason: EarnRestoreReason,
    val returnTarget: String = EARN_RETURN_TARGET,
    val preserveScroll: Boolean = false,
)
