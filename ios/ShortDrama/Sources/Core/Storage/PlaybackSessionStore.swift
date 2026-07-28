import Foundation
import Security

protocol PlaybackSessionStore: Sendable {
    func getOrCreateSessionId() throws -> String
}

protocol KeychainClient: Sendable {
    func read(service: String, account: String, accessGroup: String?) throws -> String?
    func write(value: String, service: String, account: String, accessGroup: String?) throws
    func delete(service: String, account: String, accessGroup: String?) throws
}

struct SystemKeychainClient: KeychainClient {
    func read(service: String, account: String, accessGroup: String?) throws -> String? {
        var query = baseQuery(service: service, account: account, accessGroup: accessGroup)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)

        switch status {
        case errSecSuccess:
            guard let data = item as? Data,
                  let value = String(data: data, encoding: .utf8) else {
                throw APIError.invalidResponse
            }
            return value
        case errSecItemNotFound:
            return nil
        default:
            throw APIError.invalidResponse
        }
    }

    func write(value: String, service: String, account: String, accessGroup: String?) throws {
        let data = Data(value.utf8)
        let query = baseQuery(service: service, account: account, accessGroup: accessGroup)
        let attributes: [String: Any] = [
            kSecValueData as String: data
        ]

        let updateStatus = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
        if updateStatus == errSecSuccess {
            return
        }

        if updateStatus != errSecItemNotFound {
            SecItemDelete(query as CFDictionary)
        }

        var addQuery = query
        addQuery[kSecValueData as String] = data
        let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
        guard addStatus == errSecSuccess else {
            throw APIError.invalidResponse
        }
    }

    func delete(service: String, account: String, accessGroup: String?) throws {
        let query = baseQuery(service: service, account: account, accessGroup: accessGroup)
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw APIError.invalidResponse
        }
    }

    private func baseQuery(service: String, account: String, accessGroup: String?) -> [String: Any] {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        if let accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        return query
    }
}

final class KeychainPlaybackSessionStore: PlaybackSessionStore, @unchecked Sendable {
    private enum Constants {
        static let service = "player.playback.session"
        static let account = "player.playback.session.id"
    }

    private let keychainClient: KeychainClient
    private let accessGroup: String?

    init(
        keychainClient: KeychainClient = SystemKeychainClient(),
        accessGroup: String? = nil
    ) {
        self.keychainClient = keychainClient
        self.accessGroup = accessGroup
    }

    func getOrCreateSessionId() throws -> String {
        if let existing = try keychainClient.read(
            service: Constants.service,
            account: Constants.account,
            accessGroup: accessGroup
        ), !existing.isEmpty {
            return existing
        }

        let sessionId = UUID().uuidString.lowercased()
        try keychainClient.write(
            value: sessionId,
            service: Constants.service,
            account: Constants.account,
            accessGroup: accessGroup
        )
        return sessionId
    }
}
