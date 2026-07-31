package com.djs66256.short_drama.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.djs66256.short_drama.feature.auth.ui.LoginScreen
import com.djs66256.short_drama.feature.booking.ui.BookingAssetsScreen
import com.djs66256.short_drama.feature.classification.ui.ClassificationScreen
import com.djs66256.short_drama.feature.common.ui.PlaceholderScreen
import com.djs66256.short_drama.feature.dramadetail.ui.DramaDetailScreen
import com.djs66256.short_drama.feature.earn.model.EARN_SOURCE
import com.djs66256.short_drama.feature.earn.model.EarnLoginContext
import com.djs66256.short_drama.feature.earn.model.EarnLoginResult
import com.djs66256.short_drama.feature.earn.model.EarnTaskContext
import com.djs66256.short_drama.feature.earn.model.EarnTaskPlayerResult
import com.djs66256.short_drama.feature.earn.model.EarnTaskPlayerResultReason
import com.djs66256.short_drama.feature.earn.ui.EarnLoginScreen
import com.djs66256.short_drama.feature.earn.ui.EarnScreen
import com.djs66256.short_drama.feature.home.ui.HomeScreen
import com.djs66256.short_drama.feature.mall.model.MallLoginContext
import com.djs66256.short_drama.feature.mall.model.MallLoginResult
import com.djs66256.short_drama.feature.mall.ui.MallLoginScreen
import com.djs66256.short_drama.feature.mall.ui.MallScreen
import com.djs66256.short_drama.feature.menu.ui.MenuPanelDrawer
import com.djs66256.short_drama.feature.menu.ui.MenuPanelRoute
import com.djs66256.short_drama.feature.player.ui.PlayerScreen
import com.djs66256.short_drama.feature.profile.ui.ProfileScreen
import com.djs66256.short_drama.feature.profile.ui.SettingsScreen
import com.djs66256.short_drama.feature.ranking.ui.RankingScreen
import com.djs66256.short_drama.feature.search.ui.SearchHomeScreen
import com.djs66256.short_drama.feature.search.ui.SearchResultScreen
import com.djs66256.short_drama.feature.theater.ui.TheaterScreen
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController,
    navigationViewModel: MainNavigationViewModel = hiltViewModel(),
) {
    val uiState by navigationViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var mallSearchReturnSignal by remember { mutableStateOf(0) }
    var mallLoginResultSignal by remember { mutableStateOf(0) }
    var latestMallLoginResult by remember { mutableStateOf<MallLoginResult?>(null) }
    var earnLoginResultSignal by remember { mutableStateOf(0) }
    var latestEarnLoginResult by remember { mutableStateOf<EarnLoginResult?>(null) }
    var earnTaskPlayerResultSignal by remember { mutableStateOf(0) }
    var latestEarnTaskPlayerResult by remember { mutableStateOf<EarnTaskPlayerResult?>(null) }

    LaunchedEffect(uiState.pendingRoute) {
        when (val pendingRoute = uiState.pendingRoute) {
            null -> Unit
            PendingRoute.Home -> {
                navigateToTopLevelTab(navController, TopLevelTab.HOME)
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
            PendingRoute.MenuLogin -> {
                navController.navigate(
                    AppDestination.login(
                        returnRoute = AppDestination.Route.PROFILE,
                        source = "menu_login",
                    ),
                )
                navigationViewModel.consumePendingRoute()
            }
            PendingRoute.MenuMessages -> {
                navController.navigate(AppDestination.menuMessages())
                navigationViewModel.consumePendingRoute()
            }
            PendingRoute.MenuBooking -> {
                navController.navigate(AppDestination.menuBooking())
                navigationViewModel.consumePendingRoute()
            }
            PendingRoute.MenuDownloads -> {
                navController.navigate(AppDestination.menuDownloads())
                navigationViewModel.consumePendingRoute()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                                    navigateToTopLevelTab(navController, tab)
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
                            onOpenMenu = navigationViewModel::openMenu,
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
                            onBack = {
                                mallSearchReturnSignal += 1
                                navController.popBackStack()
                            },
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
                            onRequireLogin = { returnRoute ->
                                navController.navigate(
                                    AppDestination.login(
                                        returnRoute = returnRoute,
                                        source = "ranking_booking",
                                    ),
                                )
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
                    menuPlaceholderSpecs().forEach { spec ->
                        composable(route = spec.route) {
                            PlaceholderScreen(
                                title = spec.title,
                                description = spec.description,
                            )
                        }
                    }
                    composable(route = AppDestination.Route.MENU_BOOKING) {
                        BookingAssetsScreen(
                            onBack = { navController.popBackStack() },
                            onRequireLogin = { returnRoute ->
                                navController.navigate(
                                    AppDestination.login(
                                        returnRoute = returnRoute,
                                        source = "menu_booking",
                                    ),
                                )
                            },
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
                        TheaterScreen(
                            onNavigateToRoute = { route ->
                                navigateToHomeOwnedDestination(navController, route)
                            },
                            onOpenPlay = { videoId ->
                                navController.navigate(AppDestination.play(videoId))
                            },
                        )
                    }
                }

                navigation(
                    startDestination = AppDestination.Route.MALL,
                    route = AppDestination.Graph.MALL,
                ) {
                    composable(route = AppDestination.Route.MALL) {
                        MallScreen(
                            onOpenSearch = {
                                navController.navigate(AppDestination.search())
                            },
                            onOpenMallLogin = { context: MallLoginContext ->
                                navController.navigate(
                                    AppDestination.mallLogin(
                                        productId = context.productId,
                                        returnTarget = context.returnTarget,
                                    ),
                                )
                            },
                            searchReturnSignal = mallSearchReturnSignal,
                            loginResultSignal = mallLoginResultSignal,
                            latestLoginResult = latestMallLoginResult,
                        )
                    }
                    composable(
                        route = AppDestination.Route.MALL_LOGIN,
                        arguments = listOf(
                            navArgument(AppDestination.Arg.PRODUCT_ID) {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument(AppDestination.Arg.RETURN_TARGET) {
                                type = NavType.StringType
                                defaultValue = "/mall"
                            },
                        ),
                    ) { backStackEntry ->
                        val productId = backStackEntry.arguments
                            ?.getString(AppDestination.Arg.PRODUCT_ID)
                            .orEmpty()
                        MallLoginScreen(
                            productId = productId,
                            onClose = {
                                latestMallLoginResult = MallLoginResult.CLOSED
                                mallLoginResultSignal += 1
                                navController.popBackStack()
                            },
                            onCancel = {
                                latestMallLoginResult = MallLoginResult.CANCELLED
                                mallLoginResultSignal += 1
                                navController.popBackStack()
                            },
                            onSuccess = {
                                latestMallLoginResult = MallLoginResult.SUCCESS
                                mallLoginResultSignal += 1
                                navController.popBackStack()
                            },
                        )
                    }
                }

                navigation(
                    startDestination = AppDestination.Route.EARN,
                    route = AppDestination.Graph.EARN,
                ) {
                    composable(route = AppDestination.Route.EARN) {
                        EarnScreen(
                            onOpenEarnLogin = { context: EarnLoginContext ->
                                navController.navigate(
                                    AppDestination.earnLogin(context.returnTarget),
                                )
                            },
                            onOpenEarnTaskPlayer = { context: EarnTaskContext ->
                                navController.navigate(
                                    AppDestination.earnPlay(
                                        taskId = context.taskId,
                                        source = context.source,
                                        returnTarget = context.returnTarget,
                                        videoId = context.videoId,
                                    ),
                                )
                            },
                            loginResultSignal = earnLoginResultSignal,
                            latestLoginResult = latestEarnLoginResult,
                            taskPlayerResultSignal = earnTaskPlayerResultSignal,
                            latestTaskPlayerResult = latestEarnTaskPlayerResult,
                        )
                    }
                    composable(
                        route = AppDestination.Route.EARN_LOGIN,
                        arguments = listOf(
                            navArgument(AppDestination.Arg.RETURN_TARGET) {
                                type = NavType.StringType
                                defaultValue = "/earn"
                            },
                        ),
                    ) { backStackEntry ->
                        val returnTarget = backStackEntry.arguments
                            ?.getString(AppDestination.Arg.RETURN_TARGET)
                            .orEmpty()
                        EarnLoginScreen(
                            returnTarget = returnTarget,
                            onClose = {
                                latestEarnLoginResult = EarnLoginResult.CLOSED
                                earnLoginResultSignal += 1
                                navController.popBackStack()
                            },
                            onCancel = {
                                latestEarnLoginResult = EarnLoginResult.CANCELLED
                                earnLoginResultSignal += 1
                                navController.popBackStack()
                            },
                            onSuccess = {
                                latestEarnLoginResult = EarnLoginResult.SUCCESS
                                earnLoginResultSignal += 1
                                navController.popBackStack()
                            },
                        )
                    }
                    composable(
                        route = AppDestination.Route.EARN_PLAY,
                        arguments = listOf(
                            navArgument(AppDestination.Arg.TASK_ID) {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument(AppDestination.Arg.SOURCE) {
                                type = NavType.StringType
                                defaultValue = EARN_SOURCE
                            },
                            navArgument(AppDestination.Arg.RETURN_TARGET) {
                                type = NavType.StringType
                                defaultValue = "/earn"
                            },
                            navArgument(AppDestination.Arg.VIDEO_ID) {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { backStackEntry ->
                        val taskId = backStackEntry.arguments
                            ?.getString(AppDestination.Arg.TASK_ID)
                            .orEmpty()
                        val source = backStackEntry.arguments
                            ?.getString(AppDestination.Arg.SOURCE)
                            .orEmpty()
                        val returnTarget = backStackEntry.arguments
                            ?.getString(AppDestination.Arg.RETURN_TARGET)
                            .orEmpty()
                        val videoId = backStackEntry.arguments
                            ?.getString(AppDestination.Arg.VIDEO_ID)
                            .orEmpty()
                        val taskContext = EarnTaskContext(
                            taskId = taskId,
                            source = source,
                            returnTarget = returnTarget,
                            videoId = videoId,
                        )

                        if (!taskContext.isValid()) {
                            LaunchedEffect(taskId, videoId) {
                                latestEarnTaskPlayerResult = EarnTaskPlayerResult(
                                    taskId = taskId,
                                    videoId = videoId,
                                    completed = false,
                                    reason = EarnTaskPlayerResultReason.ERROR,
                                    source = source.ifBlank { EARN_SOURCE },
                                )
                                earnTaskPlayerResultSignal += 1
                                navController.popBackStack()
                            }
                        } else {
                            val resultDeliveredKey = "earn_result_delivered"
                            var resultDelivered by remember(taskId, videoId) {
                                mutableStateOf(
                                    backStackEntry.savedStateHandle.get<Boolean>(resultDeliveredKey) == true,
                                )
                            }

                            BackHandler {
                                if (!resultDelivered) {
                                    latestEarnTaskPlayerResult = EarnTaskPlayerResult(
                                        taskId = taskId,
                                        videoId = videoId,
                                        completed = false,
                                        reason = EarnTaskPlayerResultReason.USER_EXIT,
                                        source = source,
                                    )
                                    earnTaskPlayerResultSignal += 1
                                    backStackEntry.savedStateHandle[resultDeliveredKey] = true
                                    resultDelivered = true
                                }
                                navController.popBackStack()
                            }

                            PlayerScreen(
                                navController = navController,
                                onPlaybackCompleted = {
                                    if (!resultDelivered) {
                                        latestEarnTaskPlayerResult = EarnTaskPlayerResult(
                                            taskId = taskId,
                                            videoId = videoId,
                                            completed = true,
                                            reason = EarnTaskPlayerResultReason.PLAYBACK_ENDED,
                                            source = source,
                                        )
                                        earnTaskPlayerResultSignal += 1
                                        backStackEntry.savedStateHandle[resultDeliveredKey] = true
                                        resultDelivered = true
                                    }
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }

                navigation(
                    startDestination = AppDestination.Route.PROFILE,
                    route = AppDestination.Graph.PROFILE,
                ) {
                    composable(route = AppDestination.Route.PROFILE) {
                        ProfileScreen(
                            onLoginClick = {
                                navController.navigate(AppDestination.login(returnRoute = AppDestination.Route.PROFILE))
                            },
                            onOpenSettings = {
                                navController.navigate(AppDestination.Route.SETTINGS)
                            },
                        )
                    }
                    composable(route = AppDestination.Route.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onLoggedOut = {
                                navController.navigate(AppDestination.Route.PROFILE) {
                                    popUpTo(AppDestination.Graph.PROFILE) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }

                composable(
                    route = AppDestination.Route.LOGIN,
                    arguments = listOf(
                        navArgument(AppDestination.Arg.RETURN_ROUTE) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument(AppDestination.Arg.SOURCE) {
                            type = NavType.StringType
                            defaultValue = "profile"
                        },
                    ),
                ) {
                    LoginScreen(
                        onClose = { navController.popBackStack() },
                        onLoginSuccess = { route ->
                            navController.popBackStack()
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }
        }

        MenuPanelDrawer(
            menuState = uiState.menuPanelState,
            onClose = navigationViewModel::closeMenu,
            onOpened = navigationViewModel::onMenuOpened,
            onClosedAnimationFinished = navigationViewModel::onMenuClosedAnimationFinished,
        ) {
            MenuPanelRoute(
                onNavigateFromMenu = navigationViewModel::closeMenuThenNavigate,
                onShowFeedback = { message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

internal fun shouldShowBottomBar(route: String?): Boolean {
    return !AppDestination.isPlayerRoute(route) &&
        route != AppDestination.Route.LOGIN &&
        route != AppDestination.Route.SETTINGS &&
        route != AppDestination.Route.MENU_MESSAGES &&
        route != AppDestination.Route.MENU_BOOKING &&
        route != AppDestination.Route.CLASSIFICATION
}

internal data class MenuPlaceholderSpec(
    val route: String,
    val title: String,
    val description: String,
)

internal fun menuPlaceholderSpecs(): List<MenuPlaceholderSpec> = listOf(
    MenuPlaceholderSpec(
        route = AppDestination.menuLogin(),
        title = "登录",
        description = "登录功能建设中，当前为 Native 承接页。",
    ),
    MenuPlaceholderSpec(
        route = AppDestination.menuMessages(),
        title = "我的消息",
        description = "消息能力建设中，当前为 Native 承接页。",
    ),
    MenuPlaceholderSpec(
        route = AppDestination.menuDownloads(),
        title = "我的下载",
        description = "下载功能建设中，当前为 Native 占位页。",
    ),
)

internal fun navigateToTopLevelTab(
    navController: NavHostController,
    tab: TopLevelTab,
) {
    navController.navigate(tab.graphRoute) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun navigateToHomeOwnedDestination(
    navController: NavHostController,
    route: String,
) {
    navigateToTopLevelTab(navController, TopLevelTab.HOME)
    navController.navigate(route)
}

private fun TopLevelTab.icon(): ImageVector = when (this) {
    TopLevelTab.HOME -> Icons.Filled.Home
    TopLevelTab.THEATER -> Icons.Filled.VideoLibrary
    TopLevelTab.MALL -> Icons.Filled.LocalMall
    TopLevelTab.EARN -> Icons.Filled.MonetizationOn
    TopLevelTab.PROFILE -> Icons.Filled.Person
}
