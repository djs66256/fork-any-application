package com.djs66256.short_drama.domain.model

data class AuthUser(
    val id: String,
    val phone: String,
    val displayName: String?,
    val avatarUrl: String?,
    val role: AuthRole,
    val isNewUser: Boolean,
)

enum class AuthRole {
    ADMIN,
    EDITOR,
    VIEWER,
}
