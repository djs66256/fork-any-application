package com.djs66256.short_drama.navigation

import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingType

enum class TheaterShortcutRoute(
    val title: String,
    val route: String,
) {
    Search(title = "搜索", route = AppDestination.search()),
    Classification(title = "筛选", route = AppDestination.classification()),
    Ranking(title = "排行", route = AppDestination.ranking()),
    NewReleases(title = "新剧", route = AppDestination.newReleases()),
    Booking(
        title = "预约",
        route = AppDestination.ranking(
            contentType = RankingContentType.ALL,
            type = RankingType.BOOKING,
        ),
    ),
    ;

    companion object {
        val quickEntries: List<TheaterShortcutRoute> = listOf(
            Classification,
            Ranking,
            NewReleases,
            Booking,
        )
    }
}
