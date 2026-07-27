package com.djs66256.short_drama.domain.model

const val MAX_SEARCH_QUERY_LENGTH = 50

fun normalizeSearchQuery(rawQuery: String): String = rawQuery.trim()

fun normalizeSearchQueryOrNull(rawQuery: String): String? {
    val normalized = normalizeSearchQuery(rawQuery)
    return normalized.takeIf { it.isNotEmpty() && it.length <= MAX_SEARCH_QUERY_LENGTH }
}

fun limitSearchQueryDraft(rawQuery: String): String = rawQuery.take(MAX_SEARCH_QUERY_LENGTH)
