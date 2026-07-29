package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.InteractionMessage
import com.djs66256.short_drama.domain.model.InteractionMessageType
import com.djs66256.short_drama.domain.model.MessagePage
import com.djs66256.short_drama.domain.model.MessagePreview
import com.djs66256.short_drama.domain.model.SystemMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessagePreviewDto(
    val title: String,
    val summary: String,
    @SerialName("relative_time")
    val relativeTime: String,
)

@Serializable
data class SystemMessageDto(
    val id: String,
    val title: String,
    val summary: String,
    @SerialName("sent_at")
    val sentAt: String,
)

@Serializable
data class InteractionMessageDto(
    val id: String,
    val type: String,
    val title: String,
    val summary: String,
    @SerialName("sent_at")
    val sentAt: String,
)

@Serializable
data class SystemMessageListResponseDto(
    val data: List<SystemMessageDto>,
    val pagination: PaginationDto,
)

@Serializable
data class InteractionMessageListResponseDto(
    val data: List<InteractionMessageDto>,
    val pagination: PaginationDto,
)

fun MessagePreviewDto.toDomain(): MessagePreview {
    return MessagePreview(
        title = title,
        summary = summary,
        relativeTime = relativeTime,
    )
}

fun SystemMessageDto.toDomain(): SystemMessage {
    return SystemMessage(
        id = id,
        title = title,
        summary = summary,
        sentAt = sentAt,
    )
}

fun InteractionMessageDto.toDomain(): InteractionMessage {
    return InteractionMessage(
        id = id,
        type = InteractionMessageType.fromApiValue(type),
        title = title,
        summary = summary,
        sentAt = sentAt,
    )
}

fun SystemMessageListResponseDto.toDomain(): MessagePage<SystemMessage> {
    return MessagePage(
        items = data.map { it.toDomain() },
        page = pagination.page,
        pageSize = pagination.pageSize,
        total = pagination.total,
        totalPages = pagination.totalPages,
    )
}

fun InteractionMessageListResponseDto.toDomain(): MessagePage<InteractionMessage> {
    return MessagePage(
        items = data.map { it.toDomain() },
        page = pagination.page,
        pageSize = pagination.pageSize,
        total = pagination.total,
        totalPages = pagination.totalPages,
    )
}
