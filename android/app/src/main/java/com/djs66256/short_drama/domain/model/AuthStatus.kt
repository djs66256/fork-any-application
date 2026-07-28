package com.djs66256.short_drama.domain.model

sealed interface AuthStatus {
    data object Anonymous : AuthStatus
    data object Restoring : AuthStatus
    data object Refreshing : AuthStatus
    data object Expired : AuthStatus
    data class Authenticated(val session: AuthSession) : AuthStatus
}
