import Foundation

/// Full payload used by the classification page.
struct ClassificationTagsPayload: Equatable, Sendable {
    let gender: ClassificationGender
    let dimensions: [ClassificationDimension]
}
