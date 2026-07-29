import Foundation

struct MallLoginContext: Hashable, Identifiable, Sendable {
    let source: String
    let productID: String
    let returnTarget: String

    var id: String {
        "\(source)-\(productID)-\(returnTarget)"
    }
}
