package com.djs66256.short_drama.navigation

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
    }

    object Arg {
        const val ID = com.djs66256.short_drama.navigation.Arg.ID
        const val VIDEO_ID = com.djs66256.short_drama.navigation.Arg.VIDEO_ID
        const val DRAMA_ID = com.djs66256.short_drama.navigation.Arg.DRAMA_ID
    }

    val topLevelTabs = TopLevelTab.entries
    val playerRoutes = setOf(Route.PLAY, Route.PLAYER_ALIAS)

    fun play(videoId: String): String = "play/$videoId"

    fun playerAlias(videoId: String): String = "player/$videoId"

    fun detail(dramaId: String): String = "detail/$dramaId"

    fun dramaDetailAlias(dramaId: String): String = "dramaDetail/$dramaId"

    fun isPlayerRoute(route: String?): Boolean = route in playerRoutes
}

sealed interface PendingRoute {
    data object Home : PendingRoute
    data class Play(val videoId: String) : PendingRoute
    data class Detail(val dramaId: String) : PendingRoute
}

enum class NavigationErrorCode {
    INVALID_ROUTE_PARAMS,
    UNSUPPORTED_ROUTE,
    DEEPLINK_CONTAINER_NOT_READY,
}
