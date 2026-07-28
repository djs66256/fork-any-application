package com.djs66256.short_drama.domain.model

data class TheaterPage(
    val channel: TheaterChannel,
    val items: List<TheaterDrama>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
) {
    val hasNextPage: Boolean = page < totalPages
}
