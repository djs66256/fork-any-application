import SwiftUI

struct RankingHomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: RankingViewModel

    init(isUserLoggedIn: @escaping @Sendable () -> Bool = { false }) {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: RankingViewModel(
                fetchRankingsUseCase: FetchRankingsUseCase(repository: repository),
                bookDramaUseCase: BookDramaUseCase(repository: repository),
                isUserLoggedIn: isUserLoggedIn
            )
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            RankingPrimaryTabBar(selected: viewModel.selectedContentType) { type in
                Task {
                    await viewModel.selectContentType(type)
                }
            }

            RankingSecondaryTabBar(selected: viewModel.selectedRankingType) { type in
                Task {
                    await viewModel.selectRankingType(type)
                }
            }

            RankingStateView(
                viewState: viewModel.viewState,
                selectedRankingType: viewModel.selectedRankingType,
                isAppending: viewModel.isAppending,
                appendErrorMessage: viewModel.appendErrorMessage,
                bookingErrorMessage: viewModel.bookingErrorMessage,
                onTapDrama: handleTapDrama(_:),
                onTapBooking: handleTapBooking(_:),
                onRetry: { await viewModel.retry() },
                onLoadMore: {
                    Task {
                        await viewModel.loadMoreIfNeeded()
                    }
                }
            )
        }
        .padding(.top, DesignTokens.Spacing.md)
        .navigationTitle("排行")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadIfNeeded()
        }
        .onReceive(viewModel.$routeEffect) { effect in
            guard let effect else { return }
            handle(routeEffect: effect)
            viewModel.clearRouteEffect()
        }
    }

    private func handleTapDrama(_ drama: RankingDrama) {
        guard let route = RankingRouteBuilder.playRoute(for: drama) else { return }
        router.navigate(to: route)
    }

    private func handleTapBooking(_ drama: RankingDrama) {
        Task {
            await viewModel.book(drama: drama)
        }
    }

    private func handle(routeEffect: RankingViewModel.RouteEffect) {
        switch routeEffect {
        case .requireLogin(let context):
            router.presentLogin(context: RankingRouteBuilder.loginContext(for: context))
        }
    }
}

#Preview {
    NavigationStack {
        RankingHomeView()
            .environmentObject(NavigationRouter())
    }
}
