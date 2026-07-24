package com.djs66256.short_drama.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginationDto(
    val page: Int,
    @SerialName("page_size")
    val pageSize: Int,
    val total: Int,
    @SerialName("total_pages")
    val totalPages: Int
)
