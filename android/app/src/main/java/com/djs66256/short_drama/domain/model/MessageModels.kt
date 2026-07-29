package com.djs66256.short_drama.domain.model

data class MessagePreview(
    val title: String,
    val summary: String,
    val relativeTime: String,
)

data class SystemMessage(
    val id: String,
    val title: String,
    val summary: String,
    val sentAt: String,
)

data class InteractionMessage(
    val id: String,
    val type: InteractionMessageType,
    val title: String,
    val summary: String,
    val sentAt: String,
)

enum class InteractionMessageType {
    COMMENT_REPLY,
    COMMENT_LIKE,
    SYSTEM_HINT,
    ;

    companion object {
        fun fromApiValue(value: String): InteractionMessageType = when (value.trim().lowercase()) {
            "comment_reply" -> COMMENT_REPLY
            "comment_like" -> COMMENT_LIKE
            else -> SYSTEM_HINT
        }
    }
}

data class MessagePage<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
)
