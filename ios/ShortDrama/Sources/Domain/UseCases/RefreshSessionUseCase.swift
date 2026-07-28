import Foundation

struct RefreshSessionUseCase: Sendable {
    private let repository: AuthRepositoryProtocol

    init(repository: AuthRepositoryProtocol) {
        self.repository = repository
    }

    func execute(refreshToken: String) async throws -> AuthSession {
        try await repository.refreshSession(refreshToken: refreshToken)
    }
}
