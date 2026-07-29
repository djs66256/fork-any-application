package com.djs66256.short_drama.feature.mall.model

const val MALL_SOURCE = "mall"
const val MALL_RETURN_TARGET = "/mall"
const val DEFAULT_MALL_ERROR_MESSAGE = "商城加载失败，请重试"

data class MallLoginContext(
    val source: String = MALL_SOURCE,
    val productId: String,
    val returnTarget: String = MALL_RETURN_TARGET,
) {
    fun isValid(): Boolean {
        return source == MALL_SOURCE && productId.isNotBlank() && returnTarget == MALL_RETURN_TARGET
    }
}

data class MallSearchContext(
    val source: String = MALL_SOURCE,
    val returnTarget: String = MALL_RETURN_TARGET,
) {
    fun isValid(): Boolean {
        return source == MALL_SOURCE && returnTarget == MALL_RETURN_TARGET
    }
}

sealed interface MallBridgeMessage {
    data class OpenSearch(
        val source: String,
        val returnTarget: String,
    ) : MallBridgeMessage

    data class RequestLogin(
        val context: MallLoginContext,
    ) : MallBridgeMessage

    data class Invalid(
        val type: String?,
        val reason: String,
    ) : MallBridgeMessage
}

sealed interface MallPageEvent {
    data class LoadStarted(
        val url: String? = null,
    ) : MallPageEvent

    data class LoadSucceeded(
        val url: String? = null,
    ) : MallPageEvent

    data class LoadFailed(
        val url: String? = null,
        val message: String = DEFAULT_MALL_ERROR_MESSAGE,
    ) : MallPageEvent
}

enum class MallHostAuthReason(val wireValue: String) {
    INITIAL_LOAD("initial-load"),
    LOGIN_SUCCESS("login-success"),
    LOGIN_CANCEL("login-cancel"),
    APP_RESUME("app-resume"),
}

enum class MallRestoreReason(val wireValue: String) {
    SEARCH_RETURN("search-return"),
    LOGIN_RETURN("login-return"),
    CONTAINER_RECREATED("container-recreated"),
}

data class MallHostAuthState(
    val source: String = MALL_SOURCE,
    val isLoggedIn: Boolean,
    val reason: MallHostAuthReason,
    val returnTarget: String = MALL_RETURN_TARGET,
)

data class MallRestoreContext(
    val source: String = MALL_SOURCE,
    val reason: MallRestoreReason,
    val returnTarget: String = MALL_RETURN_TARGET,
    val preserveScroll: Boolean = false,
)

sealed interface MallHostMessage {
    data class SyncAuthState(
        val payload: MallHostAuthState,
    ) : MallHostMessage

    data class RestoreContext(
        val payload: MallRestoreContext,
    ) : MallHostMessage
}

enum class MallLoginResult {
    SUCCESS,
    CANCELLED,
    CLOSED,
}
