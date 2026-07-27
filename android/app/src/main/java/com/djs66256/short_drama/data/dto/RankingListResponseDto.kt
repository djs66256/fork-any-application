package com.djs66256.short_drama.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RankingListResponseDto(
    val data: List<RankingDramaDto>,
    val pagination: PaginationDto,
)
