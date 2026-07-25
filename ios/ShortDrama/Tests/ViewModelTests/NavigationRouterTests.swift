import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct NavigationRouterTests {

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

    @Test("navigate appends child route to home stack")
    func testNavigateAppendsPath() {
        let router = NavigationRouter()
        #expect(router.pathsByTab[.home]?.count == 0)

        router.navigate(to: .player(videoId: "123"))
        #expect(router.selectedTab == .home)
        #expect(router.pathsByTab[.home]?.count == 1)
    }

    @Test("dismiss removes last path element from selected tab")
    func testDismissRemovesLast() {
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "123"))
        #expect(router.pathsByTab[.home]?.count == 1)

        router.dismiss()
        #expect(router.pathsByTab[.home]?.count == 0)
    }

    @Test("popToRoot clears target tab stack")
    func testPopToRoot() {
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "123"))
        router.navigate(to: .dramaDetail(dramaId: "456"))
        #expect(router.pathsByTab[.home]?.count == 2)

        router.popToRoot(of: .home)
        #expect(router.pathsByTab[.home]?.count == 0)
        #expect(router.selectedTab == .home)
    }

    @Test("different tab stacks stay isolated")
    func testTabStacksStayIsolated() {
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "123"))
        router.select(tab: .mall)

        #expect(router.selectedTab == .mall)
        #expect(router.pathsByTab[.home]?.count == 1)
        #expect(router.pathsByTab[.mall]?.count == 0)
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
        #expect(router.pathsByTab[.home]?.count == 0)
    }
}
