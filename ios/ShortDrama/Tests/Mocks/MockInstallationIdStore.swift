import Foundation
@testable import ShortDrama

final class MockInstallationIdStore: InstallationIdStore, @unchecked Sendable {
    var installationId = "installation-001"
    var error: Error?
    private(set) var getOrCreateInstallationIdCallCount = 0

    func getOrCreateInstallationId() throws -> String {
        getOrCreateInstallationIdCallCount += 1
        if let error {
            throw error
        }
        return installationId
    }
}

final class MockCheckInPopupDismissStore: CheckInPopupDismissStore, @unchecked Sendable {
    var dismissedDates: Set<String> = []
    private(set) var checkedDates: [String] = []
    private(set) var markedDates: [String] = []

    func isDismissed(serverDate: String) -> Bool {
        checkedDates.append(serverDate)
        return dismissedDates.contains(serverDate)
    }

    func markDismissed(serverDate: String) {
        markedDates.append(serverDate)
        dismissedDates.insert(serverDate)
    }
}
