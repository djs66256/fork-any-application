package com.djs66256.short_drama.navigation

import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingType
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
        assertEquals("ranking?contentType=all&type=hot", AppDestination.ranking())
        assertEquals("classification", AppDestination.classification())
        assertEquals("new-releases", AppDestination.newReleases())
        assertEquals("actors", AppDestination.actors())
    }

    @Test
    fun `T-02 menu placeholder routes are canonical`() {
        assertEquals("menu/login", AppDestination.menuLogin())
        assertEquals("menu/messages", AppDestination.menuMessages())
        assertEquals("menu/booking", AppDestination.menuBooking())
        assertEquals("menu/downloads", AppDestination.menuDownloads())
    }

    @Test
    fun `T-02 pending route includes menu targets`() {
        assertEquals(PendingRoute.MenuLogin, PendingRoute.MenuLogin)
        assertEquals(PendingRoute.MenuMessages, PendingRoute.MenuMessages)
        assertEquals(PendingRoute.MenuBooking, PendingRoute.MenuBooking)
        assertEquals(PendingRoute.MenuDownloads, PendingRoute.MenuDownloads)
    }

    @Test
    fun `T-07 menu messages login return route is encoded`() {
        assertEquals(
            "login?returnRoute=menu%2Fmessages&source=menu_messages",
            AppDestination.login(
                returnRoute = AppDestination.menuMessages(),
                source = "menu_messages",
            ),
        )
    }

    @Test
    fun `T-08 classification tags still reuse canonical search result route`() {
        assertEquals(
            "search/result?query=%E8%90%8C%E5%AE%9D",
            AppDestination.searchResult("萌宝"),
        )
    }

    @Test
    fun `T-10 ranking route uses canonical default query args`() {
        assertEquals(
            "ranking?contentType=all&type=hot",
            AppDestination.ranking(
                contentType = RankingContentType.ALL,
                type = RankingType.HOT,
            ),
        )
    }

    @Test
    fun `T-10 ranking route encodes selected tabs in query args`() {
        assertEquals(
            "ranking?contentType=ai&type=booking",
            AppDestination.ranking(
                contentType = RankingContentType.AI,
                type = RankingType.BOOKING,
            ),
        )
    }

    @Test
    fun `T-06 mall login route generates canonical destination`() {
        assertEquals(
            "mall/login?productId=product-001&returnTarget=%2Fmall",
            AppDestination.mallLogin(productId = "product-001", returnTarget = "/mall"),
        )
    }

    @Test
    fun `T-07 earn login route generates canonical destination`() {
        assertEquals(
            "earn/login?returnTarget=%2Fearn",
            AppDestination.earnLogin(returnTarget = "/earn"),
        )
    }

    @Test
    fun `T-07 earn play route preserves task source return target and video id`() {
        assertEquals(
            "earn/play?taskId=task-001&source=earn&returnTarget=%2Fearn&videoId=drama-001-episode-01",
            AppDestination.earnPlay(
                taskId = "task-001",
                source = "earn",
                returnTarget = "/earn",
                videoId = "drama-001-episode-01",
            ),
        )
    }

    @Test
    fun `T-10 login route encodes return route and source`() {
        assertEquals(
            "login?returnRoute=ranking%3FcontentType%3Dall%26type%3Dbooking&source=ranking_booking",
            AppDestination.login(
                returnRoute = "ranking?contentType=all&type=booking",
                source = "ranking_booking",
            ),
        )
    }

    @Test
    fun `T-10 settings route is canonical`() {
        assertEquals("settings", AppDestination.Route.SETTINGS)
    }

    @Test
    fun `feed actions reuse play and detail destinations with drama id`() {
        val dramaId = "feed-drama-001"

        assertEquals("play/feed-drama-001", AppDestination.play(dramaId))
        assertEquals("detail/feed-drama-001", AppDestination.detail(dramaId))
    }
}
