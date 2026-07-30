import SwiftUI

/// Search discovery home page.
struct SearchHomeView: View {

    @Environment(\.dismiss) private var dismiss
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
        VStack(spacing: 0) {
            header

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 28) {
                    QuickEntryGrid(entries: viewModel.quickEntries) { entry in
                        guard entry.type != .imageSearch else {
                            return
                        }
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
                .padding(.horizontal, 16)
                .padding(.top, 10)
                .padding(.bottom, 28)
            }
        }
        .background(Color(.systemBackground))
        .navigationBarBackButtonHidden()
        .toolbar(.hidden, for: .navigationBar)
        .task {
            await viewModel.loadIfNeeded()
        }
    }

    private var header: some View {
        HStack(spacing: 10) {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 21, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 26, height: 42)
            }
            .buttonStyle(.plain)

            SearchBarSection(
                query: viewModel.query,
                canSubmit: viewModel.canSubmit,
                onQueryChange: viewModel.updateQuery(_:),
                onSubmit: submitCurrentQuery
            )
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
        .padding(.bottom, 8)
        .background(Color(.systemBackground))
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
