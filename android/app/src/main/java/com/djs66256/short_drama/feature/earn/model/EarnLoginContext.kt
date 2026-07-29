package com.djs66256.short_drama.feature.earn.model

const val EARN_SOURCE = "earn"
const val EARN_RETURN_TARGET = "/earn"
const val DEFAULT_EARN_ERROR_MESSAGE = "赚钱页加载失败，请稍后重试"
const val EARN_HOST_MESSAGE_EVENT = "earn.hostMessage"

data class EarnLoginContext(
    val source: String = EARN_SOURCE,
    val returnTarget: String = EARN_RETURN_TARGET,
) {
    fun isValid(): Boolean {
        return source == EARN_SOURCE && returnTarget == EARN_RETURN_TARGET
    }
}
