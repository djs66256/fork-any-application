package com.djs66256.short_drama.core.storage

import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun encodeAuthSession(session: AuthSession, json: Json): String {
    return json.encodeToString(session.toRecord())
}

internal fun decodeAuthSession(serialized: String?, json: Json): AuthSession? {
    if (serialized.isNullOrBlank()) {
        return null
    }

    return runCatching {
        json.decodeFromString(AuthSessionRecord.serializer(), serialized).toDomain()
    }.getOrNull()
}

@Serializable
private data class AuthSessionRecord(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtIso: String,
    val user: AuthUserRecord,
)

@Serializable
private data class AuthUserRecord(
    val id: String,
    val phone: String,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val role: String,
    val isNewUser: Boolean,
)

private fun AuthSession.toRecord(): AuthSessionRecord = AuthSessionRecord(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAtIso = expiresAtIso,
    user = user.toRecord(),
)

private fun AuthUser.toRecord(): AuthUserRecord = AuthUserRecord(
    id = id,
    phone = phone,
    displayName = displayName,
    avatarUrl = avatarUrl,
    role = role.name.lowercase(),
    isNewUser = isNewUser,
)

private fun AuthSessionRecord.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAtIso = expiresAtIso,
    user = user.toDomain(),
)

private fun AuthUserRecord.toDomain(): AuthUser = AuthUser(
    id = id,
    phone = phone,
    displayName = displayName,
    avatarUrl = avatarUrl,
    role = role.toAuthRole(),
    isNewUser = isNewUser,
)

private fun String.toAuthRole(): AuthRole = when (lowercase()) {
    "admin" -> AuthRole.ADMIN
    "editor" -> AuthRole.EDITOR
    else -> AuthRole.VIEWER
}
