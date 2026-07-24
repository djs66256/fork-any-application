package com.djs66256.short_drama.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String
)

@Serializable
data class ErrorDto(
    val error: ErrorDetail
)

@Serializable
data class DramaListResponseDto(
    val data: List<DramaDto>,
    val pagination: PaginationDto
)
