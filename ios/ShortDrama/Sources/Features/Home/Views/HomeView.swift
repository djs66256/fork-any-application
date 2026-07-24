import SwiftUI

/// The main home screen of the app.
struct HomeView: View {

    @EnvironmentObject var router: NavigationRouter
    @StateObject private var viewModel: HomeViewModel

    init() {
        let repository: DramaRepositoryProtocol = DramaRepository()
        let useCase = FetchDramasUseCase(repository: repository)
        _viewModel = StateObject(wrappedValue: HomeViewModel(fetchDramasUseCase: useCase))
    }

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            Image(systemName: "play.rectangle.fill")
                .font(.system(size: 60))
                .foregroundColor(.accentColor)

            Text(viewModel.appName)
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Version \(viewModel.appVersion)")
                .font(.body)
                .foregroundColor(.secondary)

            if viewModel.isLoading {
                ProgressView()
                    .padding(.top, DesignTokens.Spacing.md)
            }

            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }
        }
        .padding()
        .task {
            await viewModel.loadDramas()
        }
    }
}

#Preview {
    HomeView()
}
