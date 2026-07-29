package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.CheckInStatus

interface CheckInRepository {
    suspend fun getCheckInStatus(): ApiResult<CheckInStatus>
    suspend fun submitCheckIn(): ApiResult<CheckInStatus>
    suspend fun getDismissedServerDate(): String?
    suspend fun dismissForServerDate(serverDate: String)
}
