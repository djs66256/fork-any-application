import Foundation

/// Query used to fetch theater feed content.
struct TheaterFeedQuery: Equatable, Sendable {
    let channel: TheaterChannel
    let page: Int
    let pageSize: Int

    init(channel: TheaterChannel = .all, page: Int = 1, pageSize: Int = 20) {
        self.channel = channel
        self.page = page
        self.pageSize = pageSize
    }
}
