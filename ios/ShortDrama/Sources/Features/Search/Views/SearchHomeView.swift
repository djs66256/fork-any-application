import SwiftUI

/// Search discovery home page.
struct SearchHomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: SearchHomeViewModel

    init() {
        let dramaRepository: DramaRepositoryProtocol = DramaRepository()
        let historyRepository: SearchHistoryRepositoryProtocol = UserDefaultsSearchHistoryRepository()
        _viewModel = StateObject(
            wrappedValue: SearchHomeViewModel(
                fetchHotSearchesUseCase: FetchHotSearchesUseCase(repository: dramaRepository),
                loadSearchHistoryUseCase: LoadSearchHistoryUseCase(repository: historyRepository),
                clearSearchHistoryUseCase: ClearSearchHistoryUseCase(repository: historyRepository)
            )
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
                SearchBarSection(
                    query: viewModel.query,
                    canSubmit: viewModel.canSubmit,
                    onQueryChange: viewModel.updateQuery(_:),
                    onSubmit: submitCurrentQuery
                )

                QuickEntryGrid(entries: viewModel.quickEntries) { entry in
                    router.navigate(to: viewModel.route(for: entry))
                }

                SearchHistorySection(
                    items: viewModel.historyItems,
                    onTapKeyword: submit(keyword:),
                    onClear: viewModel.clearHistory
                )

                HotSearchSection(
                    state: viewModel.hotSearchState,
                    onTapKeyword: submit(keyword:),
                    onRetry: { await viewModel.retryHotSearch() }
                )
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, DesignTokens.Spacing.md)
        }
        .navigationTitle("搜索发现")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadIfNeeded()
        }
    }

    private func submitCurrentQuery() {
        guard let normalized = viewModel.normalizedQuery(viewModel.query) else {
            return
        }
        router.navigate(to: .searchResult(query: normalized))
    }

    private func submit(keyword: String) {
        guard let normalized = viewModel.normalizedQuery(keyword) else {
            return
        }
        viewModel.updateQuery(normalized)
        router.navigate(to: .searchResult(query: normalized))
    }
}

#Preview {
    NavigationStack {
        SearchHomeView()
            .environmentObject(NavigationRouter())
    }
}
