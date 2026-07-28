package com.djs66256.short_drama.core.auth

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.usecase.GetCurrentUserUseCase
import com.djs66256.short_drama.domain.usecase.RefreshSessionUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthBootstrapper @Inject constructor(
    private val authStateHolder: AuthStateHolder,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
) {
    suspend fun restoreIfNeeded() {
        authStateHolder.restoreIfNeeded()
        val session = authStateHolder.currentSession() ?: return

        when (val meResult = getCurrentUserUseCase()) {
            is ApiResult.Success -> {
                authStateHolder.updateSession(session.copy(user = meResult.data))
            }
            is ApiResult.Error -> {
                if (meResult.code == AUTH_UNAUTHORIZED_CODE) {
                    val refreshToken = session.refreshToken
                    if (refreshToken.isBlank()) {
                        authStateHolder.clearSession()
                        return
                    }
                    when (refreshSessionUseCase(refreshToken)) {
                        is ApiResult.Success -> Unit
                        is ApiResult.Error, is ApiResult.Exception -> {
                            authStateHolder.markExpired()
                            authStateHolder.clearSession()
                        }
                    }
                }
            }
            is ApiResult.Exception -> Unit
        }
    }

    private companion object {
        const val AUTH_UNAUTHORIZED_CODE = "AUTH_UNAUTHORIZED"
    }
}
