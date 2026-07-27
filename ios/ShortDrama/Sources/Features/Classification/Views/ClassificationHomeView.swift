import SwiftUI

struct ClassificationHomeView: View {

    @EnvironmentObject private var router: NavigationRouter
    @StateObject private var viewModel: ClassificationViewModel
    @State private var scrollTarget: ClassificationDimensionKey?

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        _viewModel = StateObject(
            wrappedValue: ClassificationViewModel(
                fetchClassificationTagsUseCase: FetchClassificationTagsUseCase(repository: repository)
            )
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            ClassificationGenderTabBar(selectedGender: viewModel.selectedGender) { gender in
                Task {
                    await viewModel.selectGender(gender)
                }
            }

            ClassificationStateView(
                viewState: viewModel.viewState,
                onRetry: {
                    await viewModel.retry()
                },
                content: {
                    HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
                        ClassificationDimensionRail(
                            dimensions: viewModel.dimensions,
                            selectedDimension: viewModel.selectedDimension
                        ) { dimension in
                            viewModel.selectDimension(dimension)
                            scrollTarget = dimension
                        }
                        .frame(width: 92)

                        ClassificationTagSectionList(
                            dimensions: viewModel.dimensions,
                            scrollTarget: scrollTarget,
                            scrollResetSeed: viewModel.scrollResetSeed,
                            onTapTag: handleTapTag(_:),
                            onVisibleDimensionChange: viewModel.updateVisibleDimension(_:)
                        )
                    }
                }
            )
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, DesignTokens.Spacing.md)
        .navigationTitle("分类")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.loadIfNeeded()
        }
        .onChange(of: viewModel.scrollResetSeed) { _, _ in
            scrollTarget = viewModel.dimensions.first?.key
        }
    }

    private func handleTapTag(_ tag: String) {
        guard let query = viewModel.normalizedTagQuery(tag) else { return }
        router.navigate(to: .searchResult(query: query))
    }
}

#Preview {
    NavigationStack {
        ClassificationHomeView()
            .environmentObject(NavigationRouter())
    }
}
