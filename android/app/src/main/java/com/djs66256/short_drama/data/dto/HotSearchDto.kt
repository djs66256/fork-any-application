package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.HotSearchItem
import kotlinx.serialization.Serializable

@Serializable
data class HotSearchItemDto(
    val rank: Int,
    val keyword: String,
    val score: Int,
) {
    fun toDomain(): HotSearchItem = HotSearchItem(
        rank = rank,
        keyword = keyword,
        score = score,
    )
}

@Serializable
data class HotSearchListResponseDto(
    val data: List<HotSearchItemDto>,
)
