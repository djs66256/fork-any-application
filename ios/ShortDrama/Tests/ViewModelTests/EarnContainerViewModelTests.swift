import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct EarnContainerViewModelTests {

    private func makeViewModel(
        url: URL? = URL(string: "https://app.example.com/earn"),
        isLoggedIn: Bool = false,
        authToken: String? = nil,
        expiresAt: String? = nil
    ) -> EarnContainerViewModel {
        EarnContainerViewModel(
            homeURLProvider: { url },
            isUserLoggedIn: isLoggedIn,
            authToken: authToken,
            authExpiry: expiresAt
        )
    }

    @Test("T-02: earn container initial load creates configured request")
    func testInitialLoadBuildsRequest() {
        let url = URL(string: "https://app.example.com/earn")!
        let viewModel = makeViewModel(url: url)

        viewModel.loadInitialPage()

        #expect(viewModel.state == .loading)
        #expect(viewModel.currentRequest?.url == url)
        #expect(viewModel.currentURL == url)
        #expect(viewModel.lastLoadedHomeURL == url)
    }

    @Test("T-02: earn container transitions to success and injects auth snapshot")
    func testHandlePageLoadedTransitionsToSuccess() {
        let url = URL(string: "https://app.example.com/earn")!
        let viewModel = makeViewModel(
            url: url,
            isLoggedIn: true,
            authToken: "access-token",
            expiresAt: "2026-07-29T12:00:00Z"
        )

        viewModel.loadInitialPage()
        viewModel.handlePageLoaded(url: url)

        #expect(viewModel.state == .success)
        #expect(viewModel.currentURL == url)
        #expect(viewModel.hostMessage == .syncAuthState(
            EarnHostAuthState(
                source: "earn",
                isLoggedIn: true,
                reason: .initialLoad,
                returnTarget: "/earn",
                apiAccessToken: "access-token",
                expiresAt: "2026-07-29T12:00:00Z"
            )
        ))
    }

    @Test("T-02: earn container transitions to retryable error and reloads last URL")
    func testHandlePageLoadFailedAndRetry() {
        let url = URL(string: "https://app.example.com/earn")!
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

    @Test("T-03: earn requestLogin bridge emits route effect and login return syncs auth")
    func testRequestLoginBridgeAndLoginSuccess() {
        let viewModel = makeViewModel(isLoggedIn: true, authToken: "access-token")
        let context = makeEarnLoginContext()

        viewModel.handleBridgeMessage(.requestLogin(context))

        #expect(viewModel.pendingLoginContext == context)
        #expect(viewModel.routeEffect == .requestLogin(context))

        viewModel.handleLoginSuccess()

        #expect(viewModel.pendingLoginContext == nil)
        #expect(viewModel.hostMessage == .restoreContext(
            EarnRestoreContextPayload(
                source: "earn",
                reason: .loginReturn,
                returnTarget: "/earn",
                preserveScroll: true
            )
        ))
    }

    @Test("T-03: login cancellation restores earn context with earn semantics")
    func testLoginCompletionRestoresEarnContext() {
        let viewModel = makeViewModel()
        let context = makeEarnLoginContext()

        viewModel.handleBridgeMessage(.requestLogin(context))
        viewModel.handleLoginCompletion()

        #expect(viewModel.pendingLoginContext == nil)
        #expect(viewModel.hostMessage == .restoreContext(
            EarnRestoreContextPayload(
                source: "earn",
                reason: .loginReturn,
                returnTarget: "/earn",
                preserveScroll: true
            )
        ))
    }

    @Test("T-04: completed task sends earn.completeTask then restoreContext")
    func testCompletedTaskResultDispatchesCompleteThenRestore() {
        let viewModel = makeViewModel(isLoggedIn: true, authToken: "access-token")
        let taskContext = makeEarnTaskContext()
        let result = makeEarnTaskPlayerResult(
            completed: true,
            reason: .playbackEnded
        )

        viewModel.handleBridgeMessage(.openTaskPlayer(taskContext))
        viewModel.handleTaskPlayerResult(result)

        #expect(viewModel.pendingTaskContext == nil)
        #expect(viewModel.hostMessage == .completeTask(result))

        viewModel.handleTaskCompletionDispatchFinished()

        #expect(viewModel.hostMessage == .restoreContext(
            EarnRestoreContextPayload(
                source: "earn",
                reason: .taskReturn,
                returnTarget: "/earn",
                preserveScroll: true
            )
        ))
    }

    @Test("T-04: incomplete task only restores earn context")
    func testIncompleteTaskResultOnlyRestoresContext() {
        let viewModel = makeViewModel()
        let taskContext = makeEarnTaskContext()
        let result = makeEarnTaskPlayerResult(
            completed: false,
            reason: .userExit
        )

        viewModel.handleBridgeMessage(.openTaskPlayer(taskContext))
        viewModel.handleTaskPlayerResult(result)

        #expect(viewModel.pendingTaskContext == nil)
        #expect(viewModel.hostMessage == .restoreContext(
            EarnRestoreContextPayload(
                source: "earn",
                reason: .taskReturn,
                returnTarget: "/earn",
                preserveScroll: true
            )
        ))
    }

    @Test("T-05: invalid bridge payloads return nil and duplicate requests are ignored")
    func testInvalidOrDuplicateBridgeMessages() {
        let viewModel = makeViewModel()

        let invalidLogin = EarnBridgeMessage(body: [
            "type": "earn.requestLogin",
            "payload": [
                "source": "mall",
                "returnTarget": "/earn"
            ]
        ])
        let invalidTask = EarnBridgeMessage(body: [
            "type": "earn.openTaskPlayer",
            "payload": [
                "taskId": "",
                "source": "earn",
                "returnTarget": "/earn",
                "videoId": "drama-001"
            ]
        ])
        #expect(invalidLogin == nil)
        #expect(invalidTask == nil)

        let loginContext = makeEarnLoginContext()
        viewModel.handleBridgeMessage(.requestLogin(loginContext))
        viewModel.clearRouteEffect()
        viewModel.handleBridgeMessage(.requestLogin(loginContext))
        #expect(viewModel.pendingLoginContext == loginContext)
        #expect(viewModel.routeEffect == nil)

        let taskContext = makeEarnTaskContext()
        viewModel.handleBridgeMessage(.openTaskPlayer(taskContext))
        viewModel.clearRouteEffect()
        viewModel.handleBridgeMessage(.openTaskPlayer(taskContext))
        #expect(viewModel.pendingTaskContext == taskContext)
        #expect(viewModel.routeEffect == nil)
    }

    @Test("container recreated reloads home and asks H5 to restore context")
    func testHandleContainerRecreated() {
        let url = URL(string: "https://app.example.com/earn")!
        let viewModel = makeViewModel(url: url)

        viewModel.handleContainerRecreated()

        #expect(viewModel.currentRequest?.url == url)
        #expect(viewModel.state == .loading)
        #expect(viewModel.hostMessage == .restoreContext(
            EarnRestoreContextPayload(
                source: "earn",
                reason: .containerRecreated,
                returnTarget: "/earn",
                preserveScroll: false
            )
        ))
    }

    @Test("app resume syncs latest earn auth snapshot")
    func testAppResumeSyncsLatestSnapshot() {
        let viewModel = makeViewModel()
        viewModel.updateAuthSnapshot(
            isLoggedIn: true,
            authToken: "fresh-token",
            expiresAt: "2026-07-30T00:00:00Z"
        )

        viewModel.handleAppResumed()

        #expect(viewModel.hostMessage == .syncAuthState(
            EarnHostAuthState(
                source: "earn",
                isLoggedIn: true,
                reason: .appResume,
                returnTarget: "/earn",
                apiAccessToken: "fresh-token",
                expiresAt: "2026-07-30T00:00:00Z"
            )
        ))
    }
}
