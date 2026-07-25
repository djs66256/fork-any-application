import Foundation
import Testing
@testable import ShortDrama

struct DeeplinkHandlerTests {

    @Test("T-21: djsdrama://open returns .home")
    func testOpenSchemeReturnsHome() {
        let url = URL(string: "djsdrama://open")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .home)
    }

    @Test("T-22: djsdrama://play/v123 returns .player(videoId: \"v123\")")
    func testPlaySchemeReturnsPlayer() {
        let url = URL(string: "djsdrama://play/v123")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .player(videoId: "v123"))
    }

    @Test("T-23: djsdrama://drama/d456 returns .dramaDetail(dramaId: \"d456\")")
    func testDramaSchemeReturnsDramaDetail() {
        let url = URL(string: "djsdrama://drama/d456")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .dramaDetail(dramaId: "d456"))
    }

    @Test("T-24: http://evil.com returns nil")
    func testInvalidSchemeReturnsNil() {
        let url = URL(string: "http://evil.com")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == nil)
    }

    @Test("T-24: djsdrama://unknown returns nil")
    func testUnknownHostReturnsNil() {
        let url = URL(string: "djsdrama://unknown")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == nil)
    }

    @Test("djsdrama://play without video ID returns nil")
    func testPlayWithoutVideoID() {
        let url = URL(string: "djsdrama://play")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == nil)
    }

    @Test("djsdrama://drama without drama ID returns nil")
    func testDramaWithoutDramaID() {
        let url = URL(string: "djsdrama://drama")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == nil)
    }
}
