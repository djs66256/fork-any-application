package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequestDto(
    @SerialName("countryCode") val countryCode: String,
    @SerialName("phone") val phone: String,
    @SerialName("scene") val scene: String,
)

@Serializable
data class CreateAuthSessionRequestDto(
    @SerialName("countryCode") val countryCode: String,
    @SerialName("phone") val phone: String,
    @SerialName("code") val code: String,
)

@Serializable
data class RefreshAuthSessionRequestDto(
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class ApiEnvelopeDto<T>(
    @SerialName("code") val code: Int,
    @SerialName("data") val data: T,
    @SerialName("message") val message: String,
)

@Serializable
data class SendOtpPayloadDto(
    @SerialName("requestId") val requestId: String,
    @SerialName("cooldownSeconds") val cooldownSeconds: Int,
    @SerialName("expiresInSeconds") val expiresInSeconds: Int,
)

@Serializable
data class AuthSessionPayloadDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("expiresAt") val expiresAt: String,
    @SerialName("user") val user: AuthUserDto,
)

@Serializable
data class AuthUserDto(
    @SerialName("id") val id: String,
    @SerialName("phone") val phone: String,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("role") val role: String,
    @SerialName("isNewUser") val isNewUser: Boolean,
)

data class SendOtpResult(
    val requestId: String,
    val cooldownSeconds: Int,
    val expiresInSeconds: Int,
)

fun SendOtpPayloadDto.toDomain(): SendOtpResult = SendOtpResult(
    requestId = requestId,
    cooldownSeconds = cooldownSeconds,
    expiresInSeconds = expiresInSeconds,
)

fun AuthSessionPayloadDto.toDomain(): AuthSession = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAtIso = expiresAt,
    user = user.toDomain(),
)

fun AuthUserDto.toDomain(): AuthUser = AuthUser(
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
