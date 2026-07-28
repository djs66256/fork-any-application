import Foundation

struct GetCurrentUserUseCase: Sendable {
    private let repository: AuthRepositoryProtocol

    init(repository: AuthRepositoryProtocol) {
        self.repository = repository
    }

    func execute(accessToken: String) async throws -> AuthUser {
        try await repository.getCurrentUser(accessToken: accessToken)
    }
}
