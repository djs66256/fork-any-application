package com.djs66256.short_drama.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentRequestDto(
    val content: String,
)
