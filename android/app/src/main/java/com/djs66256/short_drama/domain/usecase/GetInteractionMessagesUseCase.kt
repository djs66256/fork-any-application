package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.InteractionMessage
import com.djs66256.short_drama.domain.model.MessagePage
import com.djs66256.short_drama.domain.repository.MessageRepository
import javax.inject.Inject

class GetInteractionMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): ApiResult<MessagePage<InteractionMessage>> {
        return messageRepository.getInteractionMessages(page = page, pageSize = pageSize)
    }
}
