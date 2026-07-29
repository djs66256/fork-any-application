import SwiftUI

struct MallContainerView: View {
    @EnvironmentObject private var router: NavigationRouter
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
}

#Preview {
    NavigationStack {
        MallContainerView()
            .environmentObject(NavigationRouter())
    }
}
