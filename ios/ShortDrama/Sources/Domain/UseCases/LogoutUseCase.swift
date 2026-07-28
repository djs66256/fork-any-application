import Foundation

struct LogoutUseCase: Sendable {
    private let repository: AuthRepositoryProtocol

    init(repository: AuthRepositoryProtocol) {
        self.repository = repository
    }

    func execute(accessToken: String?) async throws {
        try await repository.logout(accessToken: accessToken)
    }
}
