package com.djs66256.short_drama.feature.earn.model

enum class EarnHostAuthReason(val wireValue: String) {
    INITIAL_LOAD("initial-load"),
    LOGIN_SUCCESS("login-success"),
    LOGIN_CANCEL("login-cancel"),
    APP_RESUME("app-resume"),
}

data class EarnHostAuthState(
    val source: String = EARN_SOURCE,
    val isLoggedIn: Boolean,
    val reason: EarnHostAuthReason,
    val returnTarget: String = EARN_RETURN_TARGET,
    val apiAccessToken: String? = null,
    val expiresAt: String? = null,
)
