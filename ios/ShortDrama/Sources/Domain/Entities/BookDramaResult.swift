import Foundation

/// Result returned after booking a drama.
struct BookDramaResult: Equatable, Sendable {
    let dramaID: String
    let booked: Bool
    let bookingCount: Int
}
