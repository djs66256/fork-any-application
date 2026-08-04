import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct HomeViewModelFeaturedDramaPopupTests {

    actor ControlledFeaturedDramaPopupSleep {
        private(set) var requestedDurations: [Duration] = []
        private var continuation: CheckedContinuation<Void, Error>?

        func sleep(for duration: Duration) async throws {
            requestedDurations.append(duration)
            try await withCheckedThrowingContinuation { continuation in
                self.continuation = continuation
            }
        }

        func resume() {
            continuation?.resume(returning: ())
            continuation = nil
        }
    }

    private func makeDrama(id: String = "drama-001") -> Drama {
        Drama(
            id: id,
            title: "示例短剧",
            description: "首页卡片描述",
            coverUrl: "https://example.com/cover.jpg",
            category: "都市",
            episodeCount: 12,
            tags: ["逆袭", "甜宠"],
            rating: 8.6,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z"
        )
    }

    private func makeViewModel(
        repository: MockDramaRepository,
        featuredDramaPopupSleep: @escaping @Sendable (Duration) async throws -> Void
    ) -> HomeViewModel {
        HomeViewModel(
            fetchDramasUseCase: FetchDramasUseCase(repository: repository),
            commentRepository: MockCommentRepository(),
            fetchCheckInStatusUseCase: FetchCheckInStatusUseCase(repository: MockCheckInRepository()),
            submitCheckInUseCase: SubmitCheckInUseCase(repository: MockCheckInRepository()),
            installationIdStore: MockInstallationIdStore(),
            dismissStore: MockCheckInPopupDismissStore(),
            featuredDramaPopupSleep: featuredDramaPopupSleep
        )
    }

    @Test("T-03: featured drama popup auto hides after configured duration")
    func testFeaturedDramaPopupAutoHidesAfterDelay() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let sleep = ControlledFeaturedDramaPopupSleep()
        let viewModel = makeViewModel(
            repository: mock,
            featuredDramaPopupSleep: { duration in
                try await sleep.sleep(for: duration)
            }
        )

        await viewModel.loadIfNeeded()

        #expect(viewModel.isFeaturedDramaPopupVisible == true)
        #expect(await sleep.requestedDurations == [.seconds(3)])

        await sleep.resume()
        await Task.yield()

        #expect(viewModel.isFeaturedDramaPopupVisible == false)
    }

    @Test("T-03: featured drama popup only appears once per home entry")
    func testFeaturedDramaPopupOnlyShowsOncePerEntry() async {
        let mock = MockDramaRepository()
        mock.behavior = .success([makeDrama()])
        let sleep = ControlledFeaturedDramaPopupSleep()
        let viewModel = makeViewModel(
            repository: mock,
            featuredDramaPopupSleep: { duration in
                try await sleep.sleep(for: duration)
            }
        )

        await viewModel.loadIfNeeded()
        await sleep.resume()
        await Task.yield()

        #expect(viewModel.isFeaturedDramaPopupVisible == false)

        await viewModel.retry()

        #expect(viewModel.viewState == .content([makeDrama()]))
        #expect(viewModel.isFeaturedDramaPopupVisible == false)
        #expect(mock.fetchDramasCallCount == 2)
        #expect(await sleep.requestedDurations == [.seconds(3)])
    }
}
