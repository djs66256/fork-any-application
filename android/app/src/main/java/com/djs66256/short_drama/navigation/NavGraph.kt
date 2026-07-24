package com.djs66256.short_drama.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.djs66256.short_drama.feature.dramadetail.ui.DramaDetailScreen
import com.djs66256.short_drama.feature.home.ui.HomeScreen
import com.djs66256.short_drama.feature.player.ui.PlayerScreen

/**
 * Centralized route definitions for Jetpack Navigation Compose.
 * Supports parameterized routes and deep link URIs.
 */
object Routes {
    const val HOME = "home"

    const val PLAYER = "player/{videoId}"
    const val DRAMA_DETAIL = "dramaDetail/{dramaId}"

    fun player(videoId: String): String = "player/$videoId"
    fun dramaDetail(dramaId: String): String = "dramaDetail/$dramaId"
}

/**
 * Top-level navigation graph with deep link support.
 */
@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(
            route = Routes.HOME,
            deepLinks = listOf(
                navDeepLink { uriPattern = "djsdrama://open" }
            )
        ) {
            HomeScreen()
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("videoId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "djsdrama://player/{videoId}" }
            )
        ) {
            PlayerScreen()
        }

        composable(
            route = Routes.DRAMA_DETAIL,
            arguments = listOf(
                navArgument("dramaId") { type = NavType.StringType }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "djsdrama://drama/{dramaId}" }
            )
        ) {
            DramaDetailScreen()
        }
    }
}
