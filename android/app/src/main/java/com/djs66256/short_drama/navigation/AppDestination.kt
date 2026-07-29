package com.djs66256.short_drama.navigation

import com.djs66256.short_drama.domain.model.RankingContentType
import com.djs66256.short_drama.domain.model.RankingType
import com.djs66256.short_drama.domain.model.normalizeSearchQuery
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class TopLevelTab(
    val graphRoute: String,
    val rootRoute: String,
    val label: String,
) {
    HOME(graphRoute = Graph.HOME, rootRoute = Route.HOME, label = "首页"),
    THEATER(graphRoute = Graph.THEATER, rootRoute = Route.THEATER, label = "剧场"),
    MALL(graphRoute = Graph.MALL, rootRoute = Route.MALL, label = "商城"),
    EARN(graphRoute = Graph.EARN, rootRoute = Route.EARN, label = "赚钱"),
    PROFILE(graphRoute = Graph.PROFILE, rootRoute = Route.PROFILE, label = "我的"),
}

object Graph {
    const val HOME = "home_graph"
    const val THEATER = "theater_graph"
    const val MALL = "mall_graph"
    const val EARN = "earn_graph"
    const val PROFILE = "profile_graph"
}

object Arg {
    const val ID = "id"
    const val VIDEO_ID = "videoId"
    const val DRAMA_ID = "dramaId"
    const val QUERY = "query"
    const val CONTENT_TYPE = "contentType"
    const val TYPE = "type"
    const val PRODUCT_ID = "productId"
    const val RETURN_TARGET = "returnTarget"
}

object Route {
    const val HOME = "home"
    const val THEATER = "theater"
    const val MALL = "mall"
    const val EARN = "earn"
    const val PROFILE = "profile"
    const val PLAY = "play/{videoId}"
    const val PLAYER_ALIAS = "player/{videoId}"
    const val DETAIL = "detail/{dramaId}"
    const val DRAMA_DETAIL_ALIAS = "dramaDetail/{dramaId}"
    const val SEARCH = "search"
    const val SEARCH_RESULT = "search/result?query={query}"
    const val RANKING = "ranking?contentType={contentType}&type={type}"
    const val CLASSIFICATION = "classification"
    const val NEW_RELEASES = "new-releases"
    const val ACTORS = "actors"
    const val MALL_LOGIN = "mall/login?productId={productId}&returnTarget={returnTarget}"
    const val MENU_LOGIN = "menu/login"
    const val MENU_MESSAGES = "menu/messages"
    const val MENU_BOOKING = "menu/booking"
    const val MENU_DOWNLOADS = "menu/downloads"
}

object AppDestination {
    object Graph {
        const val HOME = com.djs66256.short_drama.navigation.Graph.HOME
        const val THEATER = com.djs66256.short_drama.navigation.Graph.THEATER
        const val MALL = com.djs66256.short_drama.navigation.Graph.MALL
        const val EARN = com.djs66256.short_drama.navigation.Graph.EARN
        const val PROFILE = com.djs66256.short_drama.navigation.Graph.PROFILE
    }

    object Route {
        const val HOME = com.djs66256.short_drama.navigation.Route.HOME
        const val THEATER = com.djs66256.short_drama.navigation.Route.THEATER
        const val MALL = com.djs66256.short_drama.navigation.Route.MALL
        const val EARN = com.djs66256.short_drama.navigation.Route.EARN
        const val PROFILE = com.djs66256.short_drama.navigation.Route.PROFILE
        const val PLAY = com.djs66256.short_drama.navigation.Route.PLAY
        const val PLAYER_ALIAS = com.djs66256.short_drama.navigation.Route.PLAYER_ALIAS
        const val DETAIL = com.djs66256.short_drama.navigation.Route.DETAIL
        const val DRAMA_DETAIL_ALIAS = com.djs66256.short_drama.navigation.Route.DRAMA_DETAIL_ALIAS
        const val SEARCH = com.djs66256.short_drama.navigation.Route.SEARCH
        const val SEARCH_RESULT = com.djs66256.short_drama.navigation.Route.SEARCH_RESULT
        const val RANKING = com.djs66256.short_drama.navigation.Route.RANKING
        const val CLASSIFICATION = com.djs66256.short_drama.navigation.Route.CLASSIFICATION
        const val NEW_RELEASES = com.djs66256.short_drama.navigation.Route.NEW_RELEASES
        const val ACTORS = com.djs66256.short_drama.navigation.Route.ACTORS
        const val MALL_LOGIN = com.djs66256.short_drama.navigation.Route.MALL_LOGIN
        const val MENU_LOGIN = com.djs66256.short_drama.navigation.Route.MENU_LOGIN
        const val MENU_MESSAGES = com.djs66256.short_drama.navigation.Route.MENU_MESSAGES
        const val MENU_BOOKING = com.djs66256.short_drama.navigation.Route.MENU_BOOKING
        const val MENU_DOWNLOADS = com.djs66256.short_drama.navigation.Route.MENU_DOWNLOADS
    }

    object Arg {
        const val ID = com.djs66256.short_drama.navigation.Arg.ID
        const val VIDEO_ID = com.djs66256.short_drama.navigation.Arg.VIDEO_ID
        const val DRAMA_ID = com.djs66256.short_drama.navigation.Arg.DRAMA_ID
        const val QUERY = com.djs66256.short_drama.navigation.Arg.QUERY
        const val CONTENT_TYPE = com.djs66256.short_drama.navigation.Arg.CONTENT_TYPE
        const val TYPE = com.djs66256.short_drama.navigation.Arg.TYPE
        const val PRODUCT_ID = com.djs66256.short_drama.navigation.Arg.PRODUCT_ID
        const val RETURN_TARGET = com.djs66256.short_drama.navigation.Arg.RETURN_TARGET
    }

    val topLevelTabs = TopLevelTab.entries
    val playerRoutes = setOf(Route.PLAY, Route.PLAYER_ALIAS)

    fun play(videoId: String): String = "play/$videoId"

    fun playerAlias(videoId: String): String = "player/$videoId"

    fun detail(dramaId: String): String = "detail/$dramaId"

    fun dramaDetailAlias(dramaId: String): String = "dramaDetail/$dramaId"

    fun isPlayerRoute(route: String?): Boolean = route in playerRoutes

    fun search(): String = Route.SEARCH

    fun searchResult(query: String): String = "search/result?query=${encodeRouteParam(normalizeSearchQuery(query))}"

    fun ranking(
        contentType: RankingContentType = RankingContentType.ALL,
        type: RankingType = RankingType.HOT,
    ): String = "ranking?contentType=${contentType.apiValue}&type=${type.apiValue}"

    fun classification(): String = Route.CLASSIFICATION

    fun newReleases(): String = Route.NEW_RELEASES

    fun actors(): String = Route.ACTORS

    fun mallLogin(productId: String, returnTarget: String): String {
        return "mall/login?productId=${encodeRouteParam(productId)}&returnTarget=${encodeRouteParam(returnTarget)}"
    }

    fun menuLogin(): String = Route.MENU_LOGIN

    fun menuMessages(): String = Route.MENU_MESSAGES

    fun menuBooking(): String = Route.MENU_BOOKING

    fun menuDownloads(): String = Route.MENU_DOWNLOADS

    private fun encodeRouteParam(rawValue: String): String {
        return URLEncoder.encode(rawValue, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
    }
}

sealed interface PendingRoute {
    data object Home : PendingRoute
    data class Play(val videoId: String) : PendingRoute
    data class Detail(val dramaId: String) : PendingRoute
    data object SearchHome : PendingRoute
    data class SearchResult(val query: String) : PendingRoute
    data object Ranking : PendingRoute
    data object Classification : PendingRoute
    data object NewReleases : PendingRoute
    data object Actors : PendingRoute
    data object MenuLogin : PendingRoute
    data object MenuMessages : PendingRoute
    data object MenuBooking : PendingRoute
    data object MenuDownloads : PendingRoute
}

enum class NavigationErrorCode {
    INVALID_ROUTE_PARAMS,
    UNSUPPORTED_ROUTE,
    DEEPLINK_CONTAINER_NOT_READY,
}
