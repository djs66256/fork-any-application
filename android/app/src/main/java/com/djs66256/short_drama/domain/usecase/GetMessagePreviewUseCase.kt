package com.djs66256.short_drama.domain.usecase

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.model.MessagePreview
import com.djs66256.short_drama.domain.repository.MessageRepository
import javax.inject.Inject

class GetMessagePreviewUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(): ApiResult<MessagePreview?> {
        return messageRepository.getMessagePreview()
    }
}
