package com.djs66256.short_drama.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeeplinkRouteParserTest {

    @Test
    fun `play deeplink maps to canonical play route`() {
        val route = DeeplinkRouteParser.parse("djsdrama://play/abc123")
        assertEquals(PendingRoute.Play("abc123"), route)
    }

    @Test
    fun `legacy player deeplink maps to canonical play route`() {
        val route = DeeplinkRouteParser.parse("djsdrama://player/abc123")
        assertEquals(PendingRoute.Play("abc123"), route)
    }

    @Test
    fun `drama deeplink maps to detail route`() {
        val route = DeeplinkRouteParser.parse("djsdrama://drama/drama456")
        assertEquals(PendingRoute.Detail("drama456"), route)
    }

    @Test
    fun `open deeplink maps to home route`() {
        val route = DeeplinkRouteParser.parse("djsdrama://open")
        assertEquals(PendingRoute.Home, route)
    }

    @Test
    fun `T-01 search deeplink maps to search home`() {
        val route = DeeplinkRouteParser.parse("djsdrama://search")
        assertEquals(PendingRoute.SearchHome, route)
    }

    @Test
    fun `T-01 search result deeplink decodes query`() {
        val route = DeeplinkRouteParser.parse("djsdrama://search/result/%E9%80%86%E8%A2%AD")
        assertEquals(PendingRoute.SearchResult("逆袭"), route)
    }

    @Test
    fun `T-01 blank search result deeplink is ignored`() {
        val route = DeeplinkRouteParser.parse("djsdrama://search/result/%20%20")
        assertNull(route)
    }

    @Test
    fun `T-01 quick entry deeplinks map to pending routes`() {
        assertEquals(PendingRoute.Ranking, DeeplinkRouteParser.parse("djsdrama://ranking"))
        assertEquals(PendingRoute.Classification, DeeplinkRouteParser.parse("djsdrama://classification"))
        assertEquals(PendingRoute.NewReleases, DeeplinkRouteParser.parse("djsdrama://new-releases"))
        assertEquals(PendingRoute.Actors, DeeplinkRouteParser.parse("djsdrama://actors"))
    }

    @Test
    fun `unknown host returns null`() {
        val route = DeeplinkRouteParser.parse("djsdrama://unknown")
        assertNull(route)
    }

    @Test
    fun `empty play parameter returns null`() {
        val route = DeeplinkRouteParser.parse("djsdrama://play")
        assertNull(route)
    }

    @Test
    fun `non djsdrama scheme returns null`() {
        val route = DeeplinkRouteParser.parse("https://example.com/play/123")
        assertNull(route)
    }
}
