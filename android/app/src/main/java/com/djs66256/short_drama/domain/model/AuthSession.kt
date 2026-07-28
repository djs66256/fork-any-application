package com.djs66256.short_drama.domain.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtIso: String,
    val user: AuthUser,
)
