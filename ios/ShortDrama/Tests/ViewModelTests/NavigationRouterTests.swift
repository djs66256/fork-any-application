import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct NavigationRouterTests {

    @Test("T-18: navigate appends to path")
    func testNavigateAppendsPath() {
        let router = NavigationRouter()
        #expect(router.path.count == 0)

        router.navigate(to: .player(videoId: "123"))
        #expect(router.path.count == 1)
    }

    @Test("T-19: dismiss removes last path element")
    func testDismissRemovesLast() {
        let router = NavigationRouter()
        router.navigate(to: .player(videoId: "123"))
        #expect(router.path.count == 1)

        router.dismiss()
        #expect(router.path.count == 0)
    }

    @Test("T-20: popToRoot clears entire path")
    func testPopToRoot() {
        let router = NavigationRouter()
        router.navigate(to: .home)
        router.navigate(to: .player(videoId: "123"))
        #expect(router.path.count == 2)

        router.popToRoot()
        #expect(router.path.count == 0)
    }

    @Test("dismiss on empty path is safe")
    func testDismissEmptyPathSafe() {
        let router = NavigationRouter()
        router.dismiss()
        #expect(router.path.count == 0)
    }

    @Test("popToRoot on empty path is safe")
    func testPopToRootEmptyPathSafe() {
        let router = NavigationRouter()
        router.popToRoot()
        #expect(router.path.count == 0)
    }
}
