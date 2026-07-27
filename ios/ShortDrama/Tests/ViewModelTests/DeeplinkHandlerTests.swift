import Foundation
@testable import ShortDrama
import Testing

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

    @Test("search host maps to search home")
    func testSearchHostReturnsSearchHome() {
        let url = URL(string: "djsdrama://search")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .searchHome)
    }

    @Test("search result deeplink maps to search result route")
    func testSearchResultHostReturnsSearchResult() {
        let url = URL(string: "djsdrama://search/result/%E9%80%86%E8%A2%AD")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .searchResult(query: "逆袭"))
    }

    @Test("search result deeplink trims decoded query")
    func testSearchResultHostTrimsDecodedQuery() {
        let url = URL(string: "djsdrama://search/result/%20%E9%80%86%E8%A2%AD%20")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .searchResult(query: "逆袭"))
    }

    @Test("ranking host maps to ranking home")
    func testRankingHostReturnsRankingHome() {
        let url = URL(string: "djsdrama://ranking")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .rankingHome)
    }

    @Test("classification host maps to classification home")
    func testClassificationHostReturnsClassificationHome() {
        let url = URL(string: "djsdrama://classification")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .classificationHome)
        #expect(route?.publicRouteName == "classification")
    }

    @Test("new releases host maps to new releases")
    func testNewReleasesHostReturnsNewReleases() {
        let url = URL(string: "djsdrama://new-releases")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .newReleases)
    }

    @Test("actors host maps to actor hub")
    func testActorsHostReturnsActorHub() {
        let url = URL(string: "djsdrama://actors")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == .actorHub)
    }

    @Test("search result deeplink without query returns nil")
    func testSearchResultWithoutQueryReturnsNil() {
        let url = URL(string: "djsdrama://search/result")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == nil)
    }

    @Test("search result deeplink with blank query returns nil")
    func testSearchResultWithBlankQueryReturnsNil() {
        let url = URL(string: "djsdrama://search/result/%20%20")!
        let route = DeeplinkHandler.handleDeepLink(url)
        #expect(route == nil)
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
