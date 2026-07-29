import Foundation

struct EarnLoginContext: Hashable, Identifiable, Sendable {
    let source: String
    let returnTarget: String

    var id: String {
        "\(source)-\(returnTarget)"
    }

    init?(source: String, returnTarget: String) {
        let normalizedSource = source.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedReturnTarget = returnTarget.trimmingCharacters(in: .whitespacesAndNewlines)

        guard normalizedSource == "earn",
              normalizedReturnTarget == "/earn" else {
            return nil
        }

        self.source = normalizedSource
        self.returnTarget = normalizedReturnTarget
    }
}
