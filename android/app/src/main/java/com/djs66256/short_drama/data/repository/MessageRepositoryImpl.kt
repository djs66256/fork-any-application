package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.data.datasource.MessageRemoteDataSource
import com.djs66256.short_drama.data.dto.toDomain
import com.djs66256.short_drama.domain.model.InteractionMessage
import com.djs66256.short_drama.domain.model.MessagePage
import com.djs66256.short_drama.domain.model.MessagePreview
import com.djs66256.short_drama.domain.model.SystemMessage
import com.djs66256.short_drama.domain.repository.MessageRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val remoteDataSource: MessageRemoteDataSource,
) : MessageRepository {
    override suspend fun getMessagePreview(): ApiResult<MessagePreview?> {
        return when (val result = remoteDataSource.getMessagePreview()) {
            is ApiResult.Success -> ApiResult.Success(result.data?.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getSystemMessages(page: Int, pageSize: Int): ApiResult<MessagePage<SystemMessage>> {
        return when (val result = remoteDataSource.getSystemMessages(page = page, pageSize = pageSize)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }

    override suspend fun getInteractionMessages(page: Int, pageSize: Int): ApiResult<MessagePage<InteractionMessage>> {
        return when (val result = remoteDataSource.getInteractionMessages(page = page, pageSize = pageSize)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toDomain())
            is ApiResult.Error -> result
            is ApiResult.Exception -> result
        }
    }
}
