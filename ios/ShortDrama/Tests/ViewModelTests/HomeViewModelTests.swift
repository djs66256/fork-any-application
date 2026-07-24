import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct HomeViewModelTests {

    // MARK: - T-14: Load success with empty array

    @Test("T-14: HomeViewModel loads successfully with empty data")
    func testLoadDramasSuccessEmpty() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([])
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadDramas()

        #expect(viewModel.isLoading == false)
        #expect(viewModel.errorMessage == nil)
    }

    // MARK: - T-15: Load failure with network error

    @Test("T-15: HomeViewModel handles network error")
    func testLoadDramasNetworkError() async {
        let mock = MockDramaRepository()
        mock.behavior = .failure(.network(underlying: URLError(.notConnectedToInternet)))
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadDramas()

        #expect(viewModel.isLoading == false)
        #expect(viewModel.errorMessage != nil)
    }

    // MARK: - T-16: Load failure with 501

    @Test("T-16: HomeViewModel handles notImplemented error")
    func testLoadDramasNotImplemented() async {
        let mock = MockDramaRepository()
        mock.behavior = .failure(.notImplemented("Service not ready"))
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        await viewModel.loadDramas()

        #expect(viewModel.isLoading == false)
        #expect(viewModel.errorMessage != nil)
        #expect(viewModel.errorMessage == "Service not ready")
    }

    // MARK: - T-17: Loading state

    @Test("T-17: HomeViewModel sets isLoading during request")
    func testLoadDramasLoadingState() async {
        let mock = MockDramaRepository()
        mock.behavior = .delayed([], 0.5)
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        // Start loading
        let task = Task {
            await viewModel.loadDramas()
        }

        // Give it a small delay to enter loading state
        try? await Task.sleep(for: .milliseconds(50))

        #expect(viewModel.isLoading == true)

        await task.value

        #expect(viewModel.isLoading == false)
    }

    // MARK: - App name/version

    @Test("HomeViewModel.appName returns value from AppConfig")
    func testAppName() {
        let mock = MockDramaRepository()
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        #expect(!viewModel.appName.isEmpty)
    }

    @Test("HomeViewModel.appVersion returns value from AppConfig")
    func testAppVersion() {
        let mock = MockDramaRepository()
        let useCase = FetchDramasUseCase(repository: mock)
        let viewModel = HomeViewModel(fetchDramasUseCase: useCase)

        #expect(!viewModel.appVersion.isEmpty)
    }
}
