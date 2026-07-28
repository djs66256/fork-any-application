package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.AuthRemoteDataSource
import com.djs66256.short_drama.data.dto.toDomain
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: AuthRemoteDataSource,
    private val authStateHolder: AuthStateHolder,
) : AuthRepository {
    override suspend fun sendOtp(
        countryCode: String,
        phone: String,
        scene: String,
    ): ApiResult<com.djs66256.short_drama.data.dto.SendOtpResult> {
        return when (val result = remoteDataSource.sendOtp(countryCode, phone, scene)) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun createSession(
        countryCode: String,
        phone: String,
        code: String,
    ): ApiResult<AuthSession> {
        return when (val result = remoteDataSource.createSession(countryCode, phone, code)) {
            is ApiResult.Success -> {
                val session = result.data.data.toDomain()
                authStateHolder.updateSession(session)
                ApiResult.Success(session)
            }
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun refreshSession(refreshToken: String): ApiResult<AuthSession> {
        return when (val result = remoteDataSource.refreshSession(refreshToken)) {
            is ApiResult.Success -> {
                val session = result.data.data.toDomain()
                authStateHolder.updateSession(session)
                ApiResult.Success(session)
            }
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getCurrentUser(): ApiResult<AuthUser> {
        return when (val result = remoteDataSource.getCurrentUser()) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun logout(): ApiResult<Unit> {
        return when (val result = remoteDataSource.logout()) {
            is ApiResult.Success -> {
                authStateHolder.clearSession()
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> {
                authStateHolder.clearSession()
                result
            }
            is ApiResult.Exception -> {
                authStateHolder.clearSession()
                result
            }
        }
    }
}
