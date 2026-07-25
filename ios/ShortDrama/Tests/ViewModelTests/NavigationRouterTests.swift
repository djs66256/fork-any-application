import Foundation
import Testing
@testable import ShortDrama

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

    @Test("router defaults to home tab with empty stacks")
    func testDefaults() {
        let router = NavigationRouter()

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.isEmpty == true)
        #expect(router.pathsByTab[.theater]?.isEmpty == true)
        #expect(router.pathsByTab[.mall]?.isEmpty == true)
        #expect(router.pendingRoute == nil)
        #expect(router.containerReady == false)
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

    @Test("navigate appends player route to home stack")
    func testNavigatePlayerAppendsPath() {
        let router = NavigationRouter()
        #expect(router.pathsByTab[.home]?.isEmpty == true)

        router.navigate(to: .player(videoId: "123"))
        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("navigate appends drama detail route to home stack")
    func testNavigateDramaDetailAppendsPath() {
        let router = NavigationRouter()

        router.navigate(to: .dramaDetail(dramaId: "456"))

        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
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
    }

    @Test("markContainerReady consumes pending route")
    func testPendingRouteConsumedWhenReady() {
        let router = NavigationRouter()
        router.enqueueDeepLink(.player(videoId: "123"))

        router.markContainerReady()

        #expect(router.containerReady == true)
        #expect(router.pendingRoute == nil)
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
