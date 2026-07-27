package com.djs66256.short_drama.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.djs66256.short_drama.feature.classification.ui.ClassificationScreen
import com.djs66256.short_drama.feature.common.ui.PlaceholderScreen
import com.djs66256.short_drama.feature.dramadetail.ui.DramaDetailScreen
import com.djs66256.short_drama.feature.home.ui.HomeScreen
import com.djs66256.short_drama.feature.player.ui.PlayerScreen
import com.djs66256.short_drama.feature.ranking.ui.RankingScreen
import com.djs66256.short_drama.feature.search.ui.SearchHomeScreen
import com.djs66256.short_drama.feature.search.ui.SearchResultScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    navigationViewModel: MainNavigationViewModel = hiltViewModel(),
) {
    val uiState by navigationViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(uiState.pendingRoute) {
        when (val pendingRoute = uiState.pendingRoute) {
            null -> Unit
            PendingRoute.Home -> {
                navController.navigate(TopLevelTab.HOME.graphRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
                navigationViewModel.consumePendingRoute()
            }
            is PendingRoute.Play -> {
                if (pendingRoute.videoId.isBlank()) {
                    navigationViewModel.rejectPendingRoute(NavigationErrorCode.INVALID_ROUTE_PARAMS)
                } else {
                    navController.navigate(AppDestination.play(pendingRoute.videoId))
                    navigationViewModel.consumePendingRoute()
                }
            }
            is PendingRoute.Detail -> {
                if (pendingRoute.dramaId.isBlank()) {
                    navigationViewModel.rejectPendingRoute(NavigationErrorCode.INVALID_ROUTE_PARAMS)
                } else {
                    navController.navigate(AppDestination.detail(pendingRoute.dramaId))
                    navigationViewModel.consumePendingRoute()
                }
            }
            PendingRoute.SearchHome -> {
                navController.navigate(AppDestination.search())
                navigationViewModel.consumePendingRoute()
            }
            is PendingRoute.SearchResult -> {
                if (pendingRoute.query.isBlank()) {
                    navigationViewModel.rejectPendingRoute(NavigationErrorCode.INVALID_ROUTE_PARAMS)
                } else {
                    navController.navigate(AppDestination.searchResult(pendingRoute.query))
                    navigationViewModel.consumePendingRoute()
                }
            }
            PendingRoute.Ranking -> {
                navController.navigate(AppDestination.ranking())
                navigationViewModel.consumePendingRoute()
            }
            PendingRoute.Classification -> {
                navController.navigate(AppDestination.classification())
                navigationViewModel.consumePendingRoute()
            }
            PendingRoute.NewReleases -> {
                navController.navigate(AppDestination.newReleases())
                navigationViewModel.consumePendingRoute()
            }
            PendingRoute.Actors -> {
                navController.navigate(AppDestination.actors())
                navigationViewModel.consumePendingRoute()
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentDestination?.route)) {
                NavigationBar {
                    AppDestination.topLevelTabs.forEach { tab ->
                        val selected = currentDestination
                            ?.hierarchy
                            ?.any { destination ->
                                destination.route == tab.graphRoute || destination.route == tab.rootRoute
                            } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.graphRoute) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon(),
                                    contentDescription = tab.label,
                                )
                            },
                            label = {
                                Text(tab.label)
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Graph.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation(
                startDestination = AppDestination.Route.HOME,
                route = AppDestination.Graph.HOME,
            ) {
                composable(route = AppDestination.Route.HOME) {
                    HomeScreen(
                        onOpenSearch = {
                            navController.navigate(AppDestination.search())
                        },
                        onOpenPlay = { videoId ->
                            navController.navigate(AppDestination.play(videoId))
                        },
                        onOpenDetail = { dramaId ->
                            navController.navigate(AppDestination.detail(dramaId))
                        },
                    )
                }
                composable(route = AppDestination.Route.SEARCH) {
                    SearchHomeScreen(
                        onBack = { navController.popBackStack() },
                        onSubmitQuery = { route -> navController.navigate(route) },
                        onOpenQuickEntry = { route -> navController.navigate(route) },
                    )
                }
                composable(
                    route = AppDestination.Route.SEARCH_RESULT,
                    arguments = listOf(
                        navArgument(AppDestination.Arg.QUERY) {
                            type = NavType.StringType
                            nullable = true
                        },
                    ),
                ) {
                    SearchResultScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPlay = { videoId -> navController.navigate(AppDestination.play(videoId)) },
                        onOpenDetail = { dramaId -> navController.navigate(AppDestination.detail(dramaId)) },
                    )
                }
                composable(
                    route = AppDestination.Route.RANKING,
                    arguments = listOf(
                        navArgument(AppDestination.Arg.CONTENT_TYPE) {
                            type = NavType.StringType
                            defaultValue = "all"
                        },
                        navArgument(AppDestination.Arg.TYPE) {
                            type = NavType.StringType
                            defaultValue = "hot"
                        },
                    ),
                ) {
                    RankingScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPlay = { videoId ->
                            navController.navigate(AppDestination.play(videoId))
                        },
                        onRequireLogin = { _ ->
                            // Login flow is not implemented in this PRD yet.
                        },
                    )
                }
                composable(route = AppDestination.Route.CLASSIFICATION) {
                    ClassificationScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSearchResult = { route ->
                            navController.navigate(route)
                        },
                    )
                }
                composable(route = AppDestination.Route.NEW_RELEASES) {
                    PlaceholderScreen(
                        title = "新剧",
                        description = "新剧功能建设中，当前为 Native 承接页。",
                    )
                }
                composable(route = AppDestination.Route.ACTORS) {
                    PlaceholderScreen(
                        title = "演员",
                        description = "演员功能建设中，当前为 Native 承接页。",
                    )
                }
                composable(
                    route = AppDestination.Route.PLAY,
                    arguments = listOf(
                        navArgument(AppDestination.Arg.VIDEO_ID) { type = NavType.StringType },
                    ),
                ) {
                    PlayerScreen()
                }
                composable(
                    route = AppDestination.Route.PLAYER_ALIAS,
                    arguments = listOf(
                        navArgument(AppDestination.Arg.VIDEO_ID) { type = NavType.StringType },
                    ),
                ) { backStackEntry ->
                    val videoId = backStackEntry.arguments?.getString(AppDestination.Arg.VIDEO_ID).orEmpty()
                    LaunchedEffect(videoId) {
                        if (videoId.isNotBlank()) {
                            navController.navigate(AppDestination.play(videoId)) {
                                popUpTo(backStackEntry.destination.route ?: AppDestination.Route.PLAYER_ALIAS) {
                                    inclusive = true
                                }
                            }
                        }
                    }
                }
                composable(
                    route = AppDestination.Route.DETAIL,
                    arguments = listOf(
                        navArgument(AppDestination.Arg.DRAMA_ID) { type = NavType.StringType },
                    ),
                ) {
                    DramaDetailScreen()
                }
                composable(
                    route = AppDestination.Route.DRAMA_DETAIL_ALIAS,
                    arguments = listOf(
                        navArgument(AppDestination.Arg.DRAMA_ID) { type = NavType.StringType },
                    ),
                ) {
                    DramaDetailScreen()
                }
            }

            navigation(
                startDestination = AppDestination.Route.THEATER,
                route = AppDestination.Graph.THEATER,
            ) {
                composable(route = AppDestination.Route.THEATER) {
                    PlaceholderScreen(
                        title = TopLevelTab.THEATER.label,
                        description = "剧场频道占位页，后续 PRD 会在这里接入真实内容。",
                    )
                }
            }

            navigation(
                startDestination = AppDestination.Route.MALL,
                route = AppDestination.Graph.MALL,
            ) {
                composable(route = AppDestination.Route.MALL) {
                    PlaceholderScreen(
                        title = TopLevelTab.MALL.label,
                        description = "商城频道占位页，后续 PRD 会在这里接入真实内容。",
                    )
                }
            }

            navigation(
                startDestination = AppDestination.Route.EARN,
                route = AppDestination.Graph.EARN,
            ) {
                composable(route = AppDestination.Route.EARN) {
                    PlaceholderScreen(
                        title = TopLevelTab.EARN.label,
                        description = "赚钱频道占位页，后续 PRD 会在这里接入真实内容。",
                    )
                }
            }

            navigation(
                startDestination = AppDestination.Route.PROFILE,
                route = AppDestination.Graph.PROFILE,
            ) {
                composable(route = AppDestination.Route.PROFILE) {
                    PlaceholderScreen(
                        title = TopLevelTab.PROFILE.label,
                        description = "我的频道占位页，后续 PRD 会在这里接入真实内容。",
                    )
                }
            }
        }
    }
}

internal fun shouldShowBottomBar(route: String?): Boolean = !AppDestination.isPlayerRoute(route)

private fun TopLevelTab.icon(): ImageVector = when (this) {
    TopLevelTab.HOME -> Icons.Filled.Home
    TopLevelTab.THEATER -> Icons.Filled.VideoLibrary
    TopLevelTab.MALL -> Icons.Filled.LocalMall
    TopLevelTab.EARN -> Icons.Filled.MonetizationOn
    TopLevelTab.PROFILE -> Icons.Filled.Person
}
