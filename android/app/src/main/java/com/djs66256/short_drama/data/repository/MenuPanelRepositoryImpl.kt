package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.MenuPanelRemoteDataSource
import com.djs66256.short_drama.data.datasource.MessageRemoteDataSource
import com.djs66256.short_drama.data.dto.toDomain
import com.djs66256.short_drama.domain.model.MessagePreview
import com.djs66256.short_drama.domain.model.RecentlyViewed
import com.djs66256.short_drama.domain.repository.MenuPanelRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuPanelRepositoryImpl @Inject constructor(
    private val remoteDataSource: MenuPanelRemoteDataSource,
    private val messageRemoteDataSource: MessageRemoteDataSource,
) : MenuPanelRepository {

    override suspend fun getRecentlyViewed(sessionId: String): ApiResult<List<RecentlyViewed>> {
        return when (val result = remoteDataSource.getRecentlyViewed(playbackSessionId = sessionId)) {
            is ApiResult.Success -> ApiResult.Success(result.data.data.items.map { it.toDomain() })
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getMessagePreview(): ApiResult<MessagePreview?> {
        return when (val result = messageRemoteDataSource.getMessagePreview()) {
            is ApiResult.Success -> ApiResult.Success(result.data?.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }
}
