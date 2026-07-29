import Foundation

struct CommentQuery: Equatable, Sendable {
    let dramaId: String
    let page: Int
    let pageSize: Int
    let sort: CommentSort
}
