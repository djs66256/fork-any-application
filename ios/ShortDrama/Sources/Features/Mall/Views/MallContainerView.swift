import SwiftUI

struct MallContainerView: View {
    @EnvironmentObject private var router: NavigationRouter
    @EnvironmentObject private var authStore: AuthStore
    @StateObject private var viewModel = MallContainerViewModel()

    var body: some View {
        ZStack {
            if let request = viewModel.currentRequest {
                MallWebView(
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
                MallContainerStateView(state: viewModel.state, onRetry: viewModel.reload)
            }
        }
        .navigationTitle("商城")
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
            guard hostMessage != nil else { return }
            DispatchQueue.main.async {
                viewModel.clearHostMessage()
            }
        }
        .onChange(of: authStore.status) { _, _ in
            syncAuthSnapshot()
            guard router.mallLoginContext == nil else { return }
            viewModel.handleAppResumed()
        }
        .onChange(of: router.pendingMallRestoreRequest) { _, _ in
            consumeRouterRestoreRequestIfNeeded()
        }
        .fullScreenCover(item: mallLoginContextBinding) { context in
            MallLoginPlaceholderView(
                context: context,
                onClose: {
                    router.dismissMallLogin(completed: false)
                },
                onCompleteLogin: {
                    router.dismissMallLogin(completed: true)
                }
            )
        }
    }

    private var mallLoginContextBinding: Binding<MallLoginContext?> {
        Binding(
            get: { router.mallLoginContext },
            set: { newValue in
                if newValue == nil, router.mallLoginContext != nil {
                    router.dismissMallLogin(completed: false)
                }
            }
        )
    }

    private func handle(routeEffect: MallContainerViewModel.RouteEffect) {
        switch routeEffect {
        case .openSearch:
            router.openSearchFromMall()
        case .requestLogin(let context):
            router.presentMallLogin(context)
        }
    }

    private func consumeRouterRestoreRequestIfNeeded() {
        guard let request = router.consumeMallRestoreRequest() else { return }
        syncAuthSnapshot()

        switch request {
        case .searchReturn:
            viewModel.handleSearchReturn()
        case .loginReturn(let completed):
            if completed {
                viewModel.handleLoginSuccess()
            } else {
                viewModel.handleLoginCompletion()
            }
        }
    }

    private func syncAuthSnapshot() {
        viewModel.updateAuthSnapshot(isLoggedIn: authStore.isAuthenticated)
    }
}

#Preview {
    NavigationStack {
        MallContainerView()
            .environmentObject(NavigationRouter())
    }
}
