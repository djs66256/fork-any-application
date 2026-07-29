import Foundation

struct SystemMessage: Equatable, Identifiable, Sendable {
    let id: String
    let title: String
    let summary: String
    let sentAt: String
}
