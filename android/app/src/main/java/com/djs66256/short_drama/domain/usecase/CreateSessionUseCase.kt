package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.repository.AuthRepository
import javax.inject.Inject

class CreateSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        countryCode: String,
        phone: String,
        code: String,
    ): ApiResult<AuthSession> {
        return authRepository.createSession(
            countryCode = countryCode,
            phone = phone,
            code = code,
        )
    }
}
