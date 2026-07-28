package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.dto.SendOtpResult
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser

interface AuthRepository {
    suspend fun sendOtp(
        countryCode: String,
        phone: String,
        scene: String,
    ): ApiResult<SendOtpResult>

    suspend fun createSession(
        countryCode: String,
        phone: String,
        code: String,
    ): ApiResult<AuthSession>

    suspend fun refreshSession(refreshToken: String): ApiResult<AuthSession>

    suspend fun getCurrentUser(): ApiResult<AuthUser>

    suspend fun logout(): ApiResult<Unit>
}
