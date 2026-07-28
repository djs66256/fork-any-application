import Foundation

struct CreateSessionUseCase: Sendable {
    private let repository: AuthRepositoryProtocol

    init(repository: AuthRepositoryProtocol) {
        self.repository = repository
    }

    func execute(phone: String, countryCode: String, code: String) async throws -> AuthSession {
        try await repository.createSession(phone: phone, countryCode: countryCode, code: code)
    }
}
