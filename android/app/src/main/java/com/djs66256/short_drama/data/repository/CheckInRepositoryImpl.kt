package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.CheckInLocalStore
import com.djs66256.short_drama.data.datasource.CheckInRemoteDataSource
import com.djs66256.short_drama.data.dto.toDomain
import com.djs66256.short_drama.domain.model.CheckInStatus
import com.djs66256.short_drama.domain.repository.CheckInRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInRepositoryImpl @Inject constructor(
    private val remoteDataSource: CheckInRemoteDataSource,
    private val localStore: CheckInLocalStore,
) : CheckInRepository {
    override suspend fun getCheckInStatus(): ApiResult<CheckInStatus> {
        val installationId = localStore.getOrCreateInstallationId()
        return when (val result = remoteDataSource.getCheckInStatus(installationId)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun submitCheckIn(): ApiResult<CheckInStatus> {
        val installationId = localStore.getOrCreateInstallationId()
        return when (val result = remoteDataSource.submitCheckIn(installationId)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getDismissedServerDate(): String? = localStore.getDismissedServerDate()

    override suspend fun dismissForServerDate(serverDate: String) {
        localStore.setDismissedServerDate(serverDate)
    }
}
