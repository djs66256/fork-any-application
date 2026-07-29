import Foundation

protocol InstallationIdStore: Sendable {
    func getOrCreateInstallationId() throws -> String
}

final class KeychainInstallationIdStore: InstallationIdStore, @unchecked Sendable {
    private enum Constants {
        static let service = "checkin.installation"
        static let account = "checkin.installation.id"
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

    func getOrCreateInstallationId() throws -> String {
        if let existing = try keychainClient.read(
            service: Constants.service,
            account: Constants.account,
            accessGroup: accessGroup
        ), !existing.isEmpty {
            return existing
        }

        let installationId = UUID().uuidString.lowercased()
        try keychainClient.write(
            value: installationId,
            service: Constants.service,
            account: Constants.account,
            accessGroup: accessGroup
        )
        return installationId
    }
}

protocol CheckInPopupDismissStore: Sendable {
    func isDismissed(serverDate: String) -> Bool
    func markDismissed(serverDate: String)
}

struct UserDefaultsCheckInPopupDismissStore: CheckInPopupDismissStore, @unchecked Sendable {
    private let defaults: UserDefaults
    private let keyPrefix: String

    init(defaults: UserDefaults = .standard, keyPrefix: String = "checkin.popup.dismissed") {
        self.defaults = defaults
        self.keyPrefix = keyPrefix
    }

    func isDismissed(serverDate: String) -> Bool {
        defaults.bool(forKey: dismissalKey(for: serverDate))
    }

    func markDismissed(serverDate: String) {
        defaults.set(true, forKey: dismissalKey(for: serverDate))
    }

    private func dismissalKey(for serverDate: String) -> String {
        "\(keyPrefix).\(serverDate)"
    }
}
