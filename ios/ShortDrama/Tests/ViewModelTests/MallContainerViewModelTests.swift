import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct MallContainerViewModelTests {

    private func makeViewModel(
        url: URL? = URL(string: "https://app.example.com/mall"),
        isLoggedIn: Bool = false
    ) -> MallContainerViewModel {
        MallContainerViewModel(homeURLProvider: { url }, isUserLoggedIn: isLoggedIn)
    }

    @Test("T-02: mall container initial load creates configured request")
    func testInitialLoadBuildsRequest() {
        let url = URL(string: "https://app.example.com/mall")!
        let viewModel = makeViewModel(url: url)

        viewModel.loadInitialPage()

        #expect(viewModel.state == .loading)
        #expect(viewModel.currentRequest?.url == url)
        #expect(viewModel.currentURL == url)
        #expect(viewModel.lastLoadedHomeURL == url)
    }

    @Test("T-02: mall container transitions to success after page load")
    func testHandlePageLoadedTransitionsToSuccess() {
        let url = URL(string: "https://app.example.com/mall")!
        let viewModel = makeViewModel(url: url)

        viewModel.loadInitialPage()
        viewModel.handlePageLoaded(url: url)

        #expect(viewModel.state == .success)
        #expect(viewModel.currentURL == url)
        #expect(viewModel.hostMessage == .syncAuthState(
            MallHostAuthState(source: "mall", isLoggedIn: false, reason: "initial-load", returnTarget: "/mall")
        ))
    }

    @Test("T-02: mall container transitions to retryable error and reloads last URL")
    func testHandlePageLoadFailedAndRetry() {
        let url = URL(string: "https://app.example.com/mall")!
        let viewModel = makeViewModel(url: url)

        viewModel.loadInitialPage()
        let initialRevision = viewModel.loadRevision
        viewModel.handlePageLoadFailed(url: url, message: "网络异常")

        #expect(viewModel.state == .error(message: "网络异常"))

        viewModel.reload()

        #expect(viewModel.state == .loading)
        #expect(viewModel.currentRequest?.url == url)
        #expect(viewModel.loadRevision == initialRevision + 1)
    }

    @Test("T-03: mall openSearch bridge emits route effect and search return restores context")
    func testOpenSearchBridgeAndRestoreContext() {
        let viewModel = makeViewModel()
        let message = MallBridgeMessage.openSearch(
            MallSearchContext(source: "mall", returnTarget: "/mall")
        )

        viewModel.handleBridgeMessage(message)

        #expect(viewModel.routeEffect == .openSearch(
            MallSearchContext(source: "mall", returnTarget: "/mall")
        ))

        viewModel.handleSearchReturn()

        #expect(viewModel.hostMessage == .restoreContext(
            MallRestoreContextPayload(
                source: "mall",
                reason: .searchReturn,
                returnTarget: "/mall",
                preserveScroll: true
            )
        ))
    }

    @Test("T-04: mall requestLogin bridge emits route effect and login return syncs auth")
    func testRequestLoginBridgeAndLoginCompletion() {
        let viewModel = makeViewModel(isLoggedIn: true)
        let context = MallLoginContext(source: "mall", productID: "product-001", returnTarget: "/mall")

        viewModel.handleBridgeMessage(.requestLogin(context))

        #expect(viewModel.pendingLoginContext == context)
        #expect(viewModel.routeEffect == .requestLogin(context))

        viewModel.handleLoginSuccess()

        #expect(viewModel.pendingLoginContext == nil)
        #expect(viewModel.hostMessage == .restoreContext(
            MallRestoreContextPayload(
                source: "mall",
                reason: .loginReturn,
                returnTarget: "/mall",
                preserveScroll: true
            )
        ))
    }

    @Test("T-05: invalid login payload returns nil and duplicate login requests are ignored")
    func testInvalidOrDuplicateLoginBridgeMessages() {
        let viewModel = makeViewModel()

        let invalid = MallBridgeMessage(body: [
            "type": "mall.requestLogin",
            "payload": [
                "source": "mall",
                "productId": "",
                "returnTarget": "/mall"
            ]
        ])
        #expect(invalid == nil)

        let context = MallLoginContext(source: "mall", productID: "product-001", returnTarget: "/mall")
        viewModel.handleBridgeMessage(.requestLogin(context))
        viewModel.clearRouteEffect()
        viewModel.handleBridgeMessage(.requestLogin(context))

        #expect(viewModel.pendingLoginContext == context)
        #expect(viewModel.routeEffect == nil)
    }

    @Test("container recreated reloads home and asks H5 to restore context")
    func testHandleContainerRecreated() {
        let url = URL(string: "https://app.example.com/mall")!
        let viewModel = makeViewModel(url: url)

        viewModel.handleContainerRecreated()

        #expect(viewModel.currentRequest?.url == url)
        #expect(viewModel.state == .loading)
        #expect(viewModel.hostMessage == .restoreContext(
            MallRestoreContextPayload(
                source: "mall",
                reason: .containerRecreated,
                returnTarget: "/mall",
                preserveScroll: false
            )
        ))
    }

    @Test("app resume syncs latest mall auth snapshot")
    func testAppResumeSyncsLatestSnapshot() {
        let viewModel = makeViewModel()
        viewModel.updateAuthSnapshot(isLoggedIn: true)

        viewModel.handleAppResumed()

        #expect(viewModel.hostMessage == .syncAuthState(
            MallHostAuthState(
                source: "mall",
                isLoggedIn: true,
                reason: "app-resume",
                returnTarget: "/mall"
            )
        ))
    }
}
