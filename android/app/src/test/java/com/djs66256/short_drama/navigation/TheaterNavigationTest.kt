package com.djs66256.short_drama.navigation

import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingType
import org.junit.Assert.assertEquals
import org.junit.Test

class TheaterNavigationTest {

    @Test
    fun `T-06 theater search shortcut uses canonical search route`() {
        assertEquals(AppDestination.search(), TheaterShortcutRoute.Search.route)
    }

    @Test
    fun `T-06 theater quick entry routes point to expected destinations`() {
        assertEquals(AppDestination.classification(), TheaterShortcutRoute.Classification.route)
        assertEquals(AppDestination.ranking(), TheaterShortcutRoute.Ranking.route)
        assertEquals(
            AppDestination.ranking(
                contentType = RankingContentType.ALL,
                type = RankingType.BOOKING,
            ),
            TheaterShortcutRoute.Booking.route,
        )
        assertEquals(AppDestination.newReleases(), TheaterShortcutRoute.NewReleases.route)
    }

    @Test
    fun `T-06 theater play route reuses canonical play destination`() {
        assertEquals("play/drama-42", AppDestination.play("drama-42"))
    }
}
