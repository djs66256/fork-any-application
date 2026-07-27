import Foundation

/// UserDefaults-backed repository for local search history.
struct UserDefaultsSearchHistoryRepository: SearchHistoryRepositoryProtocol, @unchecked Sendable {

    private enum Constants {
        static let maxItems = 10
    }

    private let userDefaults: UserDefaults
    private let storageKey: String

    init(
        userDefaults: UserDefaults = .standard,
        storageKey: String = "search.history.items"
    ) {
        self.userDefaults = userDefaults
        self.storageKey = storageKey
    }

    func load() -> [SearchHistoryItem] {
        guard let data = userDefaults.data(forKey: storageKey) else {
            return []
        }

        do {
            return try JSONDecoder().decode([SearchHistoryItem].self, from: data)
        } catch {
            userDefaults.removeObject(forKey: storageKey)
            return []
        }
    }

    func save(keyword: String) {
        guard let normalized = Self.normalize(keyword) else {
            return
        }

        var items = load().filter { $0.keyword != normalized }
        items.insert(SearchHistoryItem(keyword: normalized), at: 0)
        items = Array(items.prefix(Constants.maxItems))

        guard let data = try? JSONEncoder().encode(items) else {
            return
        }

        userDefaults.set(data, forKey: storageKey)
    }

    func clear() {
        userDefaults.removeObject(forKey: storageKey)
    }

    private static func normalize(_ keyword: String) -> String? {
        let normalized = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalized.isEmpty else {
            return nil
        }
        return normalized
    }
}
