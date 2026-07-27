package com.djs66256.short_drama.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `home route is home`() {
        assertEquals("home", AppDestination.Route.HOME)
    }

    @Test
    fun `play route generates correct path`() {
        assertEquals("play/abc123", AppDestination.play("abc123"))
    }

    @Test
    fun `player alias route generates correct path`() {
        assertEquals("player/abc123", AppDestination.playerAlias("abc123"))
    }

    @Test
    fun `detail route generates correct path`() {
        assertEquals("detail/xyz456", AppDestination.detail("xyz456"))
    }

    @Test
    fun `T-01 search route generates canonical destination`() {
        assertEquals("search", AppDestination.search())
    }

    @Test
    fun `T-01 search result route encodes query parameter`() {
        assertEquals(
            "search/result?query=%E9%80%86%E8%A2%AD%20%E5%BD%92%E6%9D%A5",
            AppDestination.searchResult(" 逆袭 归来 "),
        )
    }

    @Test
    fun `T-01 quick entry routes are canonical`() {
        assertEquals("ranking", AppDestination.ranking())
        assertEquals("classification", AppDestination.classification())
        assertEquals("new-releases", AppDestination.newReleases())
        assertEquals("actors", AppDestination.actors())
    }

    @Test
    fun `feed actions reuse play and detail destinations with drama id`() {
        val dramaId = "feed-drama-001"

        assertEquals("play/feed-drama-001", AppDestination.play(dramaId))
        assertEquals("detail/feed-drama-001", AppDestination.detail(dramaId))
    }
}
