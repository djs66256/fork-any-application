package com.djs66256.short_drama.feature.earn.model

sealed interface EarnPageEvent {
    data class LoadStarted(
        val url: String? = null,
    ) : EarnPageEvent

    data class LoadSucceeded(
        val url: String? = null,
    ) : EarnPageEvent

    data class LoadFailed(
        val url: String? = null,
        val message: String = DEFAULT_EARN_ERROR_MESSAGE,
    ) : EarnPageEvent
}
