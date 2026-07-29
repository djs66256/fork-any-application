package com.djs66256.short_drama.domain.repository

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.InteractionMessage
import com.djs66256.short_drama.domain.model.MessagePage
import com.djs66256.short_drama.domain.model.MessagePreview
import com.djs66256.short_drama.domain.model.SystemMessage

interface MessageRepository {
    suspend fun getMessagePreview(): ApiResult<MessagePreview?>
    suspend fun getSystemMessages(page: Int, pageSize: Int): ApiResult<MessagePage<SystemMessage>>
    suspend fun getInteractionMessages(page: Int, pageSize: Int): ApiResult<MessagePage<InteractionMessage>>
}
