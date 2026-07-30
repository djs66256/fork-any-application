import Foundation

struct FetchBookingAssetsUseCase: Sendable {
    private let repository: DramaRepositoryProtocol

    init(repository: DramaRepositoryProtocol) {
        self.repository = repository
    }

    func execute(query: BookingAssetQuery, accessToken: String) async throws -> BookingAssetPage {
        try await repository.fetchBookingAssets(query: query, accessToken: accessToken)
    }
}
