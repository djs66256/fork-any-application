package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.dto.SendOtpResult
import com.djs66256.short_drama.domain.repository.AuthRepository
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        countryCode: String,
        phone: String,
        scene: String,
    ): ApiResult<SendOtpResult> {
        return authRepository.sendOtp(
            countryCode = countryCode,
            phone = phone,
            scene = scene,
        )
    }
}
