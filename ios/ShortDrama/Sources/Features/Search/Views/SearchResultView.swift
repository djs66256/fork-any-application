import SwiftUI

/// Search result page.
struct SearchResultView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: SearchResultViewModel

    init(query: String) {
        let repository: DramaRepositoryProtocol = DramaRepository()
        let historyRepository: SearchHistoryRepositoryProtocol = UserDefaultsSearchHistoryRepository()
        _viewModel = StateObject(
            wrappedValue: SearchResultViewModel(
                initialQuery: query,
                searchDramasUseCase: SearchDramasUseCase(repository: repository),
                saveSearchHistoryUseCase: SaveSearchHistoryUseCase(repository: historyRepository)
            )
        )
    }

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            SearchBarSection(
                query: viewModel.draftQuery,
                canSubmit: viewModel.canSubmit,
                onQueryChange: viewModel.updateDraftQuery(_:),
                onSubmit: {
                    Task { await viewModel.submitSearch() }
                }
            )
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, DesignTokens.Spacing.md)

            SearchResultStateView(
                viewState: viewModel.viewState,
                query: viewModel.submittedQuery,
                onPlay: handlePlay(for:),
                onDetail: handleDetail(for:),
                onRetry: { await viewModel.retry() }
            )
            .padding(.horizontal, DesignTokens.Spacing.lg)
        }
        .navigationTitle("搜索结果")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadIfNeeded()
        }
    }

    private func handlePlay(for drama: Drama) {
        guard let route = HomeRouteBuilder.playerRoute(for: drama) else { return }
        router.navigate(to: route)
    }

    private func handleDetail(for drama: Drama) {
        guard let route = HomeRouteBuilder.detailRoute(for: drama) else { return }
        router.navigate(to: route)
    }
}

#Preview {
    NavigationStack {
        SearchResultView(query: "逆袭")
            .environmentObject(NavigationRouter())
    }
}
