import SwiftUI

struct RankingHomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: RankingViewModel
    @State private var loginAlertContext: RankingLoginContext?

    init(initialEntryContext: TheaterRankingEntryContext? = nil) {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: RankingViewModel(
                fetchRankingsUseCase: FetchRankingsUseCase(repository: repository),
                bookDramaUseCase: BookDramaUseCase(repository: repository),
                initialEntryContext: initialEntryContext
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
        .alert("请先登录", isPresented: isShowingLoginAlert, presenting: loginAlertContext) { _ in
            Button("我知道了", role: .cancel) {
                loginAlertContext = nil
            }
        } message: { context in
            Text(loginAlertMessage(for: context))
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
            loginAlertContext = context
        }
    }

    private var isShowingLoginAlert: Binding<Bool> {
        Binding(
            get: { loginAlertContext != nil },
            set: { isPresented in
                if !isPresented {
                    loginAlertContext = nil
                }
            }
        )
    }

    private func loginAlertMessage(for context: RankingLoginContext) -> String {
        let rankingLabel = switch context.rankingType {
        case .hot:
            "热播榜"
        case .recommend:
            "推荐榜"
        case .booking:
            "预约榜"
        }

        return "登录后即可继续在\(rankingLabel)中预约该短剧。"
    }
}

#Preview {
    NavigationStack {
        RankingHomeView()
            .environmentObject(NavigationRouter())
    }
}
