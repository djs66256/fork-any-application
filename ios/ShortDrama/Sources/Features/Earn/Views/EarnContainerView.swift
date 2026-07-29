import SwiftUI

struct EarnContainerView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel = EarnContainerViewModel()

    var body: some View {
        ZStack {
            if let request = viewModel.currentRequest {
                EarnWebView(
                    request: request,
                    loadRevision: viewModel.loadRevision,
                    hostMessage: viewModel.hostMessage,
                    onPageLoaded: viewModel.handlePageLoaded(url:),
                    onPageLoadFailed: viewModel.handlePageLoadFailed(url:message:),
                    onBridgeMessage: viewModel.handleBridgeMessage(_:)
                )
                .opacity(viewModel.state == .success ? 1 : 0.01)
            }

            if viewModel.state != .success {
                EarnContainerStateView(state: viewModel.state, onRetry: viewModel.reload)
            }
        }
        .navigationTitle("赚钱")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            syncAuthSnapshot()
            viewModel.loadInitialPage()
        }
        .onReceive(viewModel.$routeEffect) { effect in
            guard let effect else { return }
            handle(routeEffect: effect)
            viewModel.clearRouteEffect()
        }
        .onReceive(viewModel.$hostMessage) { hostMessage in
            guard let hostMessage else { return }
            DispatchQueue.main.async {
                if case .completeTask = hostMessage {
                    viewModel.handleTaskCompletionDispatchFinished()
                } else {
                    viewModel.clearHostMessage()
                }
            }
        }
        .onChange(of: authStore.status) { _, _ in
            syncAuthSnapshot()
            guard router.earnLoginContext == nil else { return }
            viewModel.handleAppResumed()
        }
        .onChange(of: router.pendingEarnRestoreRequest) { _, _ in
            consumeRouterRestoreRequestIfNeeded()
        }
        .fullScreenCover(item: earnLoginContextBinding) { context in
            EarnLoginPlaceholderView(
                context: context,
                onClose: {
                    router.dismissEarnLogin(completed: false)
                },
                onCompleteLogin: {
                    router.dismissEarnLogin(completed: true)
                }
            )
        }
    }

    private var earnLoginContextBinding: Binding<EarnLoginContext?> {
        Binding(
            get: { router.earnLoginContext },
            set: { newValue in
                if newValue == nil, router.earnLoginContext != nil {
                    router.dismissEarnLogin(completed: false)
                }
            }
        )
    }

    private func handle(routeEffect: EarnContainerViewModel.RouteEffect) {
        switch routeEffect {
        case .requestLogin(let context):
            router.presentEarnLogin(context)
        case .openTaskPlayer(let context):
            router.openPlayerFromEarn(context)
        }
    }

    private func consumeRouterRestoreRequestIfNeeded() {
        guard let request = router.consumeEarnRestoreRequest() else { return }
        syncAuthSnapshot()

        switch request {
        case .loginReturn(let completed):
            if completed {
                viewModel.handleLoginSuccess()
            } else {
                viewModel.handleLoginCompletion()
            }
        case .taskReturn(let result):
            viewModel.handleTaskPlayerResult(result)
        }
    }

    private func syncAuthSnapshot() {
        viewModel.updateAuthSnapshot(
            isLoggedIn: authStore.isAuthenticated,
            authToken: authStore.accessToken,
            expiresAt: authStore.accessTokenExpiresAt
        )
    }
}

#Preview {
    NavigationStack {
        EarnContainerView()
            .environmentObject(NavigationRouter())
            .environmentObject(AuthStore())
    }
}
