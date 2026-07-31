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
        VStack(spacing: 0) {
            header

            HStack(alignment: .top, spacing: 14) {
                ClassificationDimensionRail(
                    dimensions: viewModel.dimensions,
                    selectedDimension: viewModel.selectedDimension
                ) { dimension in
                    viewModel.selectDimension(dimension)
                    scrollTarget = dimension
                }
                .frame(width: 104)

                ClassificationStateView(
                    viewState: viewModel.viewState,
                    onRetry: {
                        await viewModel.retry()
                    },
                    content: {
                        ClassificationTagSectionList(
                            dimensions: viewModel.dimensions,
                            scrollTarget: scrollTarget,
                            scrollResetSeed: viewModel.scrollResetSeed,
                            onTapTag: handleTapTag(_:),
                            onVisibleDimensionChange: viewModel.updateVisibleDimension(_:)
                        )
                    }
                )
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
            .padding(.bottom, 12)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        }
        .background(pageBackground)
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .task {
            await viewModel.loadIfNeeded()
        }
        .onChange(of: viewModel.scrollResetSeed) { _, _ in
            scrollTarget = viewModel.dimensions.first?.key
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            Button {
                router.dismiss()
            } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 25, weight: .medium))
                    .foregroundStyle(Color.black)
                    .frame(width: 28, height: 28)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            ClassificationGenderTabBar(selectedGender: viewModel.selectedGender) { gender in
                Task {
                    await viewModel.selectGender(gender)
                }
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.top, 6)
        .padding(.bottom, 6)
        .background(pageBackground)
    }

    private var pageBackground: some View {
        Color(red: 0.95, green: 0.95, blue: 0.95)
            .ignoresSafeArea()
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
