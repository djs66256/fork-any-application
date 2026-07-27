import Foundation

/// ViewModel for the classification page.
@MainActor
final class ClassificationViewModel: ObservableObject {

    enum ViewState: Equatable {
        case loading
        case content([ClassificationDimension])
        case error(String)
    }

    private enum Constants {
        static let maxQueryLength = 50
    }

    @Published private(set) var selectedGender: ClassificationGender = .all
    @Published private(set) var selectedDimension: ClassificationDimensionKey = .eraBackground
    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var scrollResetSeed = 0

    private let fetchClassificationTagsUseCase: FetchClassificationTagsUseCase

    private var hasLoaded = false
    private var requestToken = UUID()
    private var currentDimensions: [ClassificationDimension]

    init(fetchClassificationTagsUseCase: FetchClassificationTagsUseCase) {
        self.fetchClassificationTagsUseCase = fetchClassificationTagsUseCase
        self.currentDimensions = ClassificationDimensionKey.allCases.map {
            ClassificationDimension(key: $0, name: $0.title, tags: [])
        }
    }

    var dimensions: [ClassificationDimension] {
        switch viewState {
        case .content(let dimensions):
            return dimensions
        case .loading, .error:
            return currentDimensions
        }
    }

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        await reload(gender: selectedGender, shouldForceScrollReset: false)
    }

    func selectGender(_ gender: ClassificationGender) async {
        guard gender != selectedGender else { return }
        selectedGender = gender
        await reload(gender: gender, shouldForceScrollReset: true)
    }

    func selectDimension(_ dimension: ClassificationDimensionKey) {
        guard containsDimension(dimension) else { return }
        selectedDimension = dimension
    }

    func updateVisibleDimension(_ dimension: ClassificationDimensionKey) {
        guard containsDimension(dimension) else { return }
        selectedDimension = dimension
    }

    func retry() async {
        await reload(gender: selectedGender, shouldForceScrollReset: true)
    }

    func normalizedTagQuery(_ input: String) -> String? {
        let normalized = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty, normalized.count <= Constants.maxQueryLength else {
            return nil
        }
        return normalized
    }

    private func reload(gender: ClassificationGender, shouldForceScrollReset: Bool) async {
        requestToken = UUID()
        let token = requestToken
        viewState = .loading

        do {
            let payload = try await fetchClassificationTagsUseCase.execute(gender: gender)
            guard token == requestToken else { return }

            currentDimensions = payload.dimensions
            selectedGender = payload.gender
            selectedDimension = payload.dimensions.first?.key ?? .eraBackground
            viewState = .content(payload.dimensions)

            if shouldForceScrollReset || selectedDimension == .eraBackground {
                scrollResetSeed += 1
            }
        } catch is CancellationError {
            guard token == requestToken else { return }
        } catch let error as APIError {
            guard token == requestToken else { return }
            viewState = .error(error.errorDescription ?? "分类加载失败，请重试")
        } catch {
            guard token == requestToken else { return }
            viewState = .error(error.localizedDescription)
        }
    }

    private func containsDimension(_ dimension: ClassificationDimensionKey) -> Bool {
        currentDimensions.contains(where: { $0.key == dimension })
    }
}
