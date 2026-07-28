package com.djs66256.short_drama.domain.model

private const val DEFAULT_THEATER_PAGE = 1
private const val DEFAULT_THEATER_PAGE_SIZE = 20

data class TheaterQuery(
    val channel: TheaterChannel = TheaterChannel.ALL,
    val page: Int = DEFAULT_THEATER_PAGE,
    val pageSize: Int = DEFAULT_THEATER_PAGE_SIZE,
)
