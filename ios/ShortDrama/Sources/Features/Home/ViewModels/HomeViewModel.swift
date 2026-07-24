import Foundation

/// ViewModel for the home screen.
@MainActor
final class HomeViewModel: ObservableObject {

    // MARK: - Published State

    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?

    // MARK: - Dependencies

    private let fetchDramasUseCase: FetchDramasUseCase

    // MARK: - Computed

    var appName: String {
        AppConfig.appName()
    }

    var appVersion: String {
        AppConfig.appVersion()
    }

    // MARK: - Init

    init(fetchDramasUseCase: FetchDramasUseCase) {
        self.fetchDramasUseCase = fetchDramasUseCase
    }

    // MARK: - Actions

    func loadDramas() async {
        isLoading = true
        errorMessage = nil

        do {
            _ = try await fetchDramasUseCase.execute(page: 1, pageSize: 20)
            isLoading = false
        } catch let error as APIError {
            isLoading = false
            errorMessage = error.errorDescription
        } catch {
            isLoading = false
            errorMessage = error.localizedDescription
        }
    }
}
