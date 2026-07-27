import Foundation
@testable import ShortDrama
import Testing

struct UserDefaultsSearchHistoryRepositoryTests {

    private func makeRepository(testName: String = #function) -> UserDefaultsSearchHistoryRepository {
        let suiteName = "UserDefaultsSearchHistoryRepositoryTests.\(testName)"
        let defaults = UserDefaults(suiteName: suiteName) ?? .standard
        defaults.removePersistentDomain(forName: suiteName)
        return UserDefaultsSearchHistoryRepository(
            userDefaults: defaults,
            storageKey: "search.history.items"
        )
    }

    @Test("history repository saves normalized keyword at top")
    func testSavePlacesTrimmedKeywordAtTop() {
        let repository = makeRepository()

        repository.save(keyword: "  逆袭  ")

        #expect(repository.load() == [SearchHistoryItem(keyword: "逆袭")])
    }

    @Test("history repository deduplicates and keeps newest first")
    func testSaveDeduplicatesKeywords() {
        let repository = makeRepository()

        repository.save(keyword: "豪门")
        repository.save(keyword: "逆袭")
        repository.save(keyword: "  豪门  ")

        #expect(repository.load() == [
            SearchHistoryItem(keyword: "豪门"),
            SearchHistoryItem(keyword: "逆袭")
        ])
    }

    @Test("history repository caps results at ten items")
    func testSaveCapsAtTenItems() {
        let repository = makeRepository()

        (1...11).forEach { index in
            repository.save(keyword: "词\(index)")
        }

        let items = repository.load()
        #expect(items.count == 10)
        #expect(items.first?.keyword == "词11")
        #expect(items.last?.keyword == "词2")
    }

    @Test("history repository ignores blank keywords")
    func testSaveIgnoresBlankKeywords() {
        let repository = makeRepository()

        repository.save(keyword: "   ")

        #expect(repository.load().isEmpty)
    }

    @Test("history repository clears all items")
    func testClearRemovesAllItems() {
        let repository = makeRepository()
        repository.save(keyword: "逆袭")
        repository.save(keyword: "豪门")

        repository.clear()

        #expect(repository.load().isEmpty)
    }

    @Test("history repository clears corrupted payload and returns empty")
    func testLoadClearsCorruptedPayload() {
        let suiteName = "UserDefaultsSearchHistoryRepositoryTests.corrupted"
        let defaults = UserDefaults(suiteName: suiteName) ?? .standard
        defaults.removePersistentDomain(forName: suiteName)
        defaults.set(Data("bad-json".utf8), forKey: "search.history.items")
        let repository = UserDefaultsSearchHistoryRepository(
            userDefaults: defaults,
            storageKey: "search.history.items"
        )

        let items = repository.load()

        #expect(items.isEmpty)
        #expect(defaults.data(forKey: "search.history.items") == nil)
    }
}
