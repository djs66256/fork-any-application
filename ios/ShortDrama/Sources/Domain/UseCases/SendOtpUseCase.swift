import Foundation

struct SendOtpUseCase: Sendable {
    private let repository: AuthRepositoryProtocol

    init(repository: AuthRepositoryProtocol) {
        self.repository = repository
    }

    func execute(phone: String, countryCode: String, scene: String) async throws -> SendOtpResult {
        try await repository.sendOtp(phone: phone, countryCode: countryCode, scene: scene)
    }
}
