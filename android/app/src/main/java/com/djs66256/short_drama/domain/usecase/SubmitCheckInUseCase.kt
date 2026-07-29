package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.CheckInStatus
import com.djs66256.short_drama.domain.repository.CheckInRepository
import javax.inject.Inject

class SubmitCheckInUseCase @Inject constructor(
    private val checkInRepository: CheckInRepository,
) {
    suspend operator fun invoke(): ApiResult<CheckInStatus> {
        return checkInRepository.submitCheckIn()
    }
}
