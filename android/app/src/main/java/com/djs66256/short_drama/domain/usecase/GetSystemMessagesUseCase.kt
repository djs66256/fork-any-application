package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.MessagePage
import com.djs66256.short_drama.domain.model.SystemMessage
import com.djs66256.short_drama.domain.repository.MessageRepository
import javax.inject.Inject

class GetSystemMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): ApiResult<MessagePage<SystemMessage>> {
        return messageRepository.getSystemMessages(page = page, pageSize = pageSize)
    }
}
