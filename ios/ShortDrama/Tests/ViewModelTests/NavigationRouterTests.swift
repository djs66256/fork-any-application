@testable import ShortDrama
import Testing

@MainActor
struct NavigationRouterTests {}

@MainActor
extension NavigationRouterTests {
    @Test("router defaults to home tab with empty stacks")
    func testDefaults() {
        let router = NavigationRouter()

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.isEmpty == true)
        #expect(router.pathsByTab[.theater]?.isEmpty == true)
        #expect(router.pathsByTab[.mall]?.isEmpty == true)
        #expect(router.pathsByTab[.earn]?.isEmpty == true)
        #expect(router.pendingRoute == nil)
        #expect(router.containerReady == false)
        #expect(router.menuPanelState == .closed)
        #expect(router.pendingMenuNavigation == nil)
    }

    @Test("menu placeholder routes belong to home tab and messages uses canonical route name")
    func testMenuPlaceholderRoutesBelongToHomeTab() {
        #expect(AppRoute.menuPlaceholder(kind: .login).owningTab == .home)
        #expect(AppRoute.messages.publicRouteName == "messages")
        #expect(AppRoute.messages.owningTab == .home)
    }

    @Test("search discovery routes belong to home tab")
    func testSearchRoutesBelongToHomeTab() {
        #expect(AppRoute.searchHome.owningTab == .home)
        #expect(AppRoute.searchResult(query: "逆袭").owningTab == .home)
        #expect(AppRoute.rankingHome.owningTab == .home)
        #expect(AppRoute.classificationHome.owningTab == .home)
        #expect(AppRoute.newReleases.owningTab == .home)
        #expect(AppRoute.actorHub.owningTab == .home)
    }

    @Test("mall login route belongs to mall tab and keeps public naming")
    func testMallLoginRouteBelongsToMallTab() {
        let route = AppRoute.mallLogin(
            context: MallLoginContext(
                source: "mall",
                productID: "product-001",
                returnTarget: "/mall"
            )
        )

        #expect(route.owningTab == .mall)
        #expect(route.publicRouteName == "mall/login")
    }

    @Test("T-01: earn routes belong to earn tab and keep public naming")
    func testEarnRoutesBelongToEarnTab() {
        let loginContext = makeEarnLoginContext()
        let taskContext = makeEarnTaskContext()

        #expect(AppRoute.earnLogin(context: loginContext).owningTab == .earn)
        #expect(AppRoute.earnLogin(context: loginContext).publicRouteName == "earn/login")
        #expect(AppRoute.earnPlayer(context: taskContext).owningTab == .earn)
        #expect(AppRoute.earnPlayer(context: taskContext).publicRouteName == "earn/player")
    }

    @Test("T-10: theater ranking context is consumed once")
    func testTheaterRankingContextConsumesOnce() {
        let router = NavigationRouter()
        let context = TheaterRankingEntryContext(contentType: .all, rankingType: .booking)

        router.openRanking(from: context)

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(router.consumeTheaterRankingEntryContext() == context)
        #expect(router.consumeTheaterRankingEntryContext() == nil)
    }

    @Test("search discovery routes expose canonical public names")
    func testSearchRoutesPublicNames() {
        #expect(AppRoute.searchHome.publicRouteName == "search")
        #expect(AppRoute.searchResult(query: "逆袭").publicRouteName == "search/result")
        #expect(AppRoute.rankingHome.publicRouteName == "ranking")
        #expect(AppRoute.classificationHome.publicRouteName == "classification")
        #expect(AppRoute.newReleases.publicRouteName == "new-releases")
        #expect(AppRoute.actorHub.publicRouteName == "actors")
        #expect(AppRoute.settings.publicRouteName == "profile/settings")
    }

    @Test("classification keeps existing route semantics and search result reuse")
    func testClassificationRouteSemanticsRemainStable() {
        #expect(AppRoute.classificationHome.owningTab == .home)
        #expect(AppRoute.classificationHome.publicRouteName == "classification")
        #expect(AppRoute.searchResult(query: "萌宝") == .searchResult(query: "萌宝"))
        #expect(AppRoute.settings.owningTab == .profile)
    }

    @Test("home route builder creates player route from drama id")
    func testHomeRouteBuilderPlayerRoute() {
        let route = HomeRouteBuilder.playerRoute(for: makeTestDrama(id: "play-001"))

        #expect(route == .player(videoId: "play-001"))
    }

    @Test("home route builder creates detail route from drama id")
    func testHomeRouteBuilderDetailRoute() {
        let route = HomeRouteBuilder.detailRoute(for: makeTestDrama(id: "detail-001"))

        #expect(route == .dramaDetail(dramaId: "detail-001"))
    }

    @Test("home route builder rejects empty ids")
    func testHomeRouteBuilderRejectsEmptyIds() {
        let drama = makeTestDrama(id: "")

        #expect(HomeRouteBuilder.playerRoute(for: drama) == nil)
        #expect(HomeRouteBuilder.detailRoute(for: drama) == nil)
    }

    @Test("ranking route builder creates player route from ranking drama id")
    func testRankingRouteBuilderCreatesPlayerRoute() {
        let route = RankingRouteBuilder.playRoute(for: makeTestRankingDrama(id: "ranking-play-001"))

        #expect(route == .player(videoId: "ranking-play-001"))
    }

    @Test("ranking route builder rejects empty ids")
    func testRankingRouteBuilderRejectsEmptyIDs() {
        #expect(RankingRouteBuilder.playRoute(for: makeTestRankingDrama(id: "")) == nil)
    }

    @Test("ranking route builder converts ranking login context to unified login context")
    func testRankingRouteBuilderCreatesUnifiedLoginContext() {
        let loginContext = RankingRouteBuilder.loginContext(
            for: RankingLoginContext(
                source: "ranking",
                contentType: .all,
                rankingType: .booking,
                dramaID: "booking-001"
            )
        )

        #expect(loginContext.source == .rankingBooking)
        #expect(loginContext.returnRoute == .rankingHome)
        #expect(loginContext.rankingContext?.dramaID == "booking-001")
    }

    @Test("player route keeps legacy videoId naming while representing dramaId")
    func testPlayerRouteKeepsLegacyVideoIdNaming() {
        let route = AppRoute.player(videoId: "drama-123")

        if case .player(let videoId) = route {
            #expect(videoId == "drama-123")
        } else {
            Issue.record("Expected player route")
        }
        #expect(route.publicRouteName == "play")
    }
}

@MainActor
extension NavigationRouterTests {
    @Test("navigate appends drama detail route to home stack")
    func testNavigateDramaDetailAppendsPath() {
        let router = NavigationRouter()

        router.navigate(to: .dramaDetail(dramaId: "456"))

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("navigate appends search home route to home stack")
    func testNavigateSearchHomeAppendsPath() {
        let router = NavigationRouter()

        router.navigate(to: .searchHome)

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("navigate appends search result route to home stack")
    func testNavigateSearchResultAppendsPath() {
        let router = NavigationRouter()

        router.navigate(to: .searchResult(query: "逆袭"))

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("navigate appends discovery placeholder route to home stack")
    func testNavigateDiscoveryPlaceholderRoutesAppendPath() {
        let router = NavigationRouter()

        router.navigate(to: .rankingHome)
        router.navigate(to: .classificationHome)
        router.navigate(to: .newReleases)
        router.navigate(to: .actorHub)

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 4)
    }

    @Test("dismiss removes last path element from selected tab")
    func testDismissRemovesLast() {
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "123"))
        #expect(router.pathsByTab[.home]?.count == 1)

        router.dismiss()

        #expect(router.pathsByTab[.home]?.isEmpty == true)
    }

    @Test("popToRoot clears target tab stack")
    func testPopToRoot() {
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "123"))
        router.navigate(to: .dramaDetail(dramaId: "456"))
        #expect(router.pathsByTab[.home]?.count == 2)

        router.popToRoot(of: .home)

        #expect(router.pathsByTab[.home]?.isEmpty == true)
        #expect(router.selectedTab == .home)
    }

    @Test("different tab stacks stay isolated")
    func testTabStacksStayIsolated() {
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "123"))
        router.select(tab: .mall)

        #expect(router.selectedTab == .mall)
        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(router.pathsByTab[.mall]?.isEmpty == true)
        #expect(router.menuPanelState == .closed)
    }

    @Test("dismiss on empty path is safe")
    func testDismissEmptyPathSafe() {
        let router = NavigationRouter()

        router.dismiss()

        #expect(router.pathsByTab[.home]?.isEmpty == true)
    }
}

@MainActor
extension NavigationRouterTests {
    @Test("markContainerReady consumes pending search route")
    func testPendingSearchRouteConsumedWhenReady() {
        let router = NavigationRouter()
        router.enqueueDeepLink(.searchResult(query: "逆袭"))

        router.markContainerReady()

        #expect(router.containerReady == true)
        #expect(router.pendingRoute == nil)
        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("mall search opens search home and restores mall context on return")
    func testOpenSearchFromMallAndRestoreContext() {
        let router = NavigationRouter()

        router.openSearchFromMall()

        #expect(router.selectedTab == .home)
        #expect(router.isPresentingSearchFromMall == true)
        #expect(router.pathsByTab[.home]?.count == 1)

        router.restoreMallContextAfterSearch()

        #expect(router.selectedTab == .mall)
        #expect(router.pathsByTab[.home]?.isEmpty == true)
        #expect(router.consumeMallRestoreRequest() == .searchReturn)
        #expect(router.isPresentingSearchFromMall == false)
    }

    @Test("mall login presentation and dismissal restore mall tab")
    func testMallLoginPresentationAndDismissal() {
        let router = NavigationRouter()
        let context = MallLoginContext(
            source: "mall",
            productID: "product-001",
            returnTarget: "/mall"
        )

        router.presentMallLogin(context)

        #expect(router.selectedTab == .mall)
        #expect(router.mallLoginContext == context)

        router.dismissMallLogin(completed: true)

        #expect(router.mallLoginContext == nil)
        #expect(router.selectedTab == .mall)
        #expect(router.consumeMallRestoreRequest() == .loginReturn(completed: true))
    }

    @Test("T-03: earn login presentation and dismissal restore earn tab")
    func testEarnLoginPresentationAndDismissal() {
        let router = NavigationRouter()
        let context = makeEarnLoginContext()

        router.presentEarnLogin(context)

        #expect(router.selectedTab == .earn)
        #expect(router.earnLoginContext == context)

        router.dismissEarnLogin(completed: true)

        #expect(router.earnLoginContext == nil)
        #expect(router.selectedTab == .earn)
        #expect(router.consumeEarnRestoreRequest() == .loginReturn(completed: true))
    }

    @Test("T-04: openPlayerFromEarn pushes earn route and task result is consumed once")
    func testOpenPlayerFromEarnAndConsumeResult() {
        let router = NavigationRouter()
        let context = makeEarnTaskContext()
        let result = makeEarnTaskPlayerResult(
            completed: true,
            reason: .playbackEnded
        )

        router.openPlayerFromEarn(context)

        #expect(router.selectedTab == .earn)
        #expect(router.pathsByTab[.earn]?.count == 1)

        router.finishEarnTaskPlayer(result: result)

        #expect(router.selectedTab == .earn)
        #expect(router.consumeEarnTaskPlayerResult() == result)
        #expect(router.consumeEarnTaskPlayerResult() == nil)
        #expect(router.consumeEarnRestoreRequest() == .taskReturn(result))
    }

    @Test("menu panel opens only on home tab")
    func testOpenMenuPanelOnHomeTab() {
        let router = NavigationRouter()

        router.openMenuPanel()
        #expect(router.menuPanelState == .open)
        #expect(router.isMenuPanelVisible == true)

        router.select(tab: .mall)
        router.openMenuPanel()
        #expect(router.menuPanelState == .closed)
        #expect(router.isMenuPanelVisible == false)
    }

    @Test("menu panel close transitions to closing and closed")
    func testCloseMenuPanelAndMarkDidClose() {
        let router = NavigationRouter()
        router.openMenuPanel()

        router.closeMenuPanel()
        #expect(router.menuPanelState == .closing)
        #expect(router.isMenuPanelVisible == true)

        router.markMenuPanelDidClose()
        #expect(router.menuPanelState == .closed)
        #expect(router.isMenuPanelVisible == false)
    }

    @Test("message navigation waits until panel close completes")
    func testCloseMenuPanelThenNavigateToMessages() {
        let router = NavigationRouter()
        router.openMenuPanel()

        router.closeMenuPanelThenNavigate(to: .messages)

        #expect(router.menuPanelState == .closing)
        #expect(router.pendingMenuNavigation == .messages)
        #expect(router.pathsByTab[.home]?.isEmpty == true)

        router.markMenuPanelDidClose()

        #expect(router.pendingMenuNavigation == nil)
        #expect(router.menuPanelState == .closed)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("menu login closes panel and presents real login flow")
    func testCloseMenuPanelThenPresentLogin() {
        let router = NavigationRouter()
        router.openMenuPanel()

        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .login))

        #expect(router.menuPanelState == .closing)
        #expect(router.pendingMenuNavigation == .menuPlaceholder(kind: .login))

        router.markMenuPanelDidClose()

        #expect(router.pendingMenuNavigation == nil)
        #expect(router.menuPanelState == .closed)
        #expect(router.presentedLoginContext?.source == .profileEntry)
        #expect(router.presentedLoginContext?.returnRoute == .settings)
    }

    @Test("player navigation waits until panel close completes")
    func testCloseMenuPanelThenNavigateToPlayer() {
        let router = NavigationRouter()
        router.openMenuPanel()

        router.closeMenuPanelThenNavigate(to: .player(videoId: "drama-001"))
        router.markMenuPanelDidClose()

        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(router.menuPanelState == .closed)
    }

    @Test("closing ignores duplicate navigation requests")
    func testClosingIgnoresDuplicateNavigationRequests() {
        let router = NavigationRouter()
        router.openMenuPanel()

        router.closeMenuPanelThenNavigate(to: .messages)
        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .downloads))

        #expect(router.pendingMenuNavigation == .messages)

        router.markMenuPanelDidClose()

        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(router.pendingMenuNavigation == nil)
    }

    @Test("router can present and cancel login context")
    func testPresentAndCancelLogin() {
        let router = NavigationRouter()
        let context = LoginInterceptionContext(source: .profileEntry)

        router.presentLogin(context: context)
        #expect(router.presentedLoginContext == context)

        router.cancelLogin()
        #expect(router.presentedLoginContext == nil)
    }

    @Test("router completes login to profile tab when no return route exists")
    func testCompleteLoginDefaultsToProfile() {
        let router = NavigationRouter()
        router.presentLogin(context: LoginInterceptionContext(source: .profileEntry))

        router.completeLogin()

        #expect(router.presentedLoginContext == nil)
        #expect(router.selectedTab == .profile)
    }

    @Test("router completes login and keeps ranking route semantics")
    func testCompleteLoginWithRankingReturnRoute() {
        let router = NavigationRouter()
        router.presentLogin(
            context: LoginInterceptionContext(
                source: .rankingBooking,
                returnRoute: .rankingHome
            )
        )

        router.completeLogin()

        #expect(router.presentedLoginContext == nil)
        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("router completes login and opens settings when requested")
    func testCompleteLoginWithSettingsReturnRoute() {
        let router = NavigationRouter()
        router.presentLogin(
            context: LoginInterceptionContext(
                source: .profileEntry,
                returnRoute: .settings
            )
        )

        router.completeLogin()

        #expect(router.presentedLoginContext == nil)
        #expect(router.selectedTab == .profile)
        #expect(router.pathsByTab[.profile]?.count == 1)
    }

    @Test("router completes login and stays in messages route when requested")
    func testCompleteLoginWithMessagesReturnRoute() {
        let router = NavigationRouter()
        router.navigate(to: .messages)
        router.presentLogin(context: LoginInterceptionContext(source: .messagesEntry, returnRoute: .messages))

        router.completeLogin()

        #expect(router.presentedLoginContext == nil)
        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("dismiss on empty path is safe")
    func testDismissEmptyPathSafe() {
        let router = NavigationRouter()
        router.dismiss()
        #expect(router.pathsByTab[.home]?.isEmpty == true)
    }
}
