package com.djs66256.short_drama.feature.search.model

import com.djs66256.short_drama.navigation.AppDestination

enum class SearchQuickEntryType {
    RANKING,
    NEW_RELEASES,
    CLASSIFICATION,
    ACTORS,
}

data class SearchQuickEntry(
    val type: SearchQuickEntryType,
    val title: String,
    val route: String,
)

fun defaultSearchQuickEntries(): List<SearchQuickEntry> = listOf(
    SearchQuickEntry(
        type = SearchQuickEntryType.RANKING,
        title = "排行",
        route = AppDestination.ranking(),
    ),
    SearchQuickEntry(
        type = SearchQuickEntryType.NEW_RELEASES,
        title = "新剧",
        route = AppDestination.newReleases(),
    ),
    SearchQuickEntry(
        type = SearchQuickEntryType.CLASSIFICATION,
        title = "分类",
        route = AppDestination.classification(),
    ),
    SearchQuickEntry(
        type = SearchQuickEntryType.ACTORS,
        title = "演员",
        route = AppDestination.actors(),
    ),
)
