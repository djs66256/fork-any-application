import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct NavigationRouterTests {

    private func makeDrama(id: String = "drama-001") -> Drama {
        Drama(
            id: id,
            title: "示例短剧",
            description: "首页卡片描述",
            coverUrl: "https://example.com/cover.jpg",
            category: "都市",
            episodeCount: 12,
            tags: ["逆袭"],
            rating: 8.6,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z"
        )
    }

    private func makeRankingDrama(id: String = "ranking-001") -> RankingDrama {
        RankingDrama(
            id: id,
            title: "排行短剧",
            description: "排行榜短剧描述",
            coverUrl: "https://example.com/ranking.jpg",
            category: "都市",
            episodeCount: 68,
            tags: ["逆袭"],
            rating: 8.9,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z",
            contentType: .liveAction,
            playCount: 98210,
            bookingCount: 820,
            recommendationScore: 58930.6,
            isBooked: false
        )
    }

    @Test("router defaults to home tab with empty stacks")
    func testDefaults() {
        let router = NavigationRouter()

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.isEmpty == true)
        #expect(router.pathsByTab[.theater]?.isEmpty == true)
        #expect(router.pathsByTab[.mall]?.isEmpty == true)
        #expect(router.pendingRoute == nil)
        #expect(router.containerReady == false)
        #expect(router.menuPanelState == .closed)
        #expect(router.pendingMenuNavigation == nil)
    }

    @Test("menu placeholder routes belong to home tab and keep public naming")
    func testMenuPlaceholderRoutesBelongToHomeTab() {
        #expect(AppRoute.menuPlaceholder(kind: .login).owningTab == .home)
        #expect(AppRoute.menuPlaceholder(kind: .messages).publicRouteName == "menu/placeholder")
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
            context: MallLoginContext(source: "mall", productID: "product-001", returnTarget: "/mall")
        )

        #expect(route.owningTab == .mall)
        #expect(route.publicRouteName == "mall/login")
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
    }

    @Test("classification keeps existing route semantics and search result reuse")
    func testClassificationRouteSemanticsRemainStable() {
        #expect(AppRoute.classificationHome.owningTab == .home)
        #expect(AppRoute.classificationHome.publicRouteName == "classification")
        #expect(AppRoute.searchResult(query: "萌宝") == .searchResult(query: "萌宝"))
    }

    @Test("home route builder creates player route from drama id")
    func testHomeRouteBuilderPlayerRoute() {
        let route = HomeRouteBuilder.playerRoute(for: makeDrama(id: "play-001"))

        #expect(route == .player(videoId: "play-001"))
    }

    @Test("home route builder creates detail route from drama id")
    func testHomeRouteBuilderDetailRoute() {
        let route = HomeRouteBuilder.detailRoute(for: makeDrama(id: "detail-001"))

        #expect(route == .dramaDetail(dramaId: "detail-001"))
    }

    @Test("home route builder rejects empty ids")
    func testHomeRouteBuilderRejectsEmptyIds() {
        let drama = makeDrama(id: "")

        #expect(HomeRouteBuilder.playerRoute(for: drama) == nil)
        #expect(HomeRouteBuilder.detailRoute(for: drama) == nil)
    }

    @Test("ranking route builder creates player route from ranking drama id")
    func testRankingRouteBuilderCreatesPlayerRoute() {
        let route = RankingRouteBuilder.playRoute(for: makeRankingDrama(id: "ranking-play-001"))

        #expect(route == .player(videoId: "ranking-play-001"))
    }

    @Test("ranking route builder rejects empty ids")
    func testRankingRouteBuilderRejectsEmptyIDs() {
        #expect(RankingRouteBuilder.playRoute(for: makeRankingDrama(id: "")) == nil)
    }

    @Test("navigate appends player route to home stack")
    func testNavigatePlayerAppendsPath() {
        let router = NavigationRouter()
        #expect(router.pathsByTab[.home]?.isEmpty == true)

        router.navigate(to: .player(videoId: "123"))
        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
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
        let context = MallLoginContext(source: "mall", productID: "product-001", returnTarget: "/mall")

        router.presentMallLogin(context)

        #expect(router.selectedTab == .mall)
        #expect(router.mallLoginContext == context)

        router.dismissMallLogin(completed: true)

        #expect(router.mallLoginContext == nil)
        #expect(router.selectedTab == .mall)
        #expect(router.consumeMallRestoreRequest() == .loginReturn(completed: true))
    }

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

    @Test("placeholder navigation waits until panel close completes")
    func testCloseMenuPanelThenNavigateToPlaceholder() {
        let router = NavigationRouter()
        router.openMenuPanel()

        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .login))

        #expect(router.menuPanelState == .closing)
        #expect(router.pendingMenuNavigation == .menuPlaceholder(kind: .login))
        #expect(router.pathsByTab[.home]?.isEmpty == true)

        router.markMenuPanelDidClose()

        #expect(router.pendingMenuNavigation == nil)
        #expect(router.menuPanelState == .closed)
        #expect(router.pathsByTab[.home]?.count == 1)
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

        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .messages))
        router.closeMenuPanelThenNavigate(to: .menuPlaceholder(kind: .downloads))

        #expect(router.pendingMenuNavigation == .menuPlaceholder(kind: .messages))

        router.markMenuPanelDidClose()

        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(router.pendingMenuNavigation == nil)
    }

    @Test("dismiss on empty path is safe")
    func testDismissEmptyPathSafe() {
        let router = NavigationRouter()
        router.dismiss()
        #expect(router.pathsByTab[.home]?.isEmpty == true)
    }
}
