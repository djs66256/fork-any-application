import Foundation

final class KeychainAuthSessionStore: AuthSessionStore, @unchecked Sendable {
    private enum Constants {
        static let service = "auth.session"
        static let account = "auth.session.payload"
    }

    private let keychainClient: KeychainClient
    private let accessGroup: String?
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    init(
        keychainClient: KeychainClient = SystemKeychainClient(),
        accessGroup: String? = nil,
        encoder: JSONEncoder = JSONEncoder(),
        decoder: JSONDecoder = JSONDecoder()
    ) {
        self.keychainClient = keychainClient
        self.accessGroup = accessGroup
        self.encoder = encoder
        self.decoder = decoder
    }

    func load() throws -> AuthSession? {
        guard let payload = try keychainClient.read(
            service: Constants.service,
            account: Constants.account,
            accessGroup: accessGroup
        ), !payload.isEmpty else {
            return nil
        }

        do {
            return try decoder.decode(AuthSession.self, from: Data(payload.utf8))
        } catch {
            try clear()
            return nil
        }
    }

    func save(_ session: AuthSession) throws {
        let data = try encoder.encode(session)
        guard let payload = String(data: data, encoding: .utf8) else {
            throw APIError.invalidResponse
        }

        try keychainClient.write(
            value: payload,
            service: Constants.service,
            account: Constants.account,
            accessGroup: accessGroup
        )
    }

    func clear() throws {
        try keychainClient.delete(
            service: Constants.service,
            account: Constants.account,
            accessGroup: accessGroup
        )
    }
}
