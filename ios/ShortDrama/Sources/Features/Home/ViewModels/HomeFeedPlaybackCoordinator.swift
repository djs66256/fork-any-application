import Foundation

@MainActor
final class HomeFeedPlaybackCoordinator: ObservableObject {
    typealias PlayerViewModelFactory = @MainActor (Drama) -> PlayerViewModel

    struct ItemState {
        let drama: Drama
        let playerViewModel: PlayerViewModel
    }

    @Published private var itemsByDramaID: [Drama.ID: ItemState] = [:]
    @Published private(set) var activeDramaID: Drama.ID?

    private let makePlayerViewModel: PlayerViewModelFactory

    init(makePlayerViewModel: @escaping PlayerViewModelFactory) {
        self.makePlayerViewModel = makePlayerViewModel
    }

    convenience init() {
        self.init(makePlayerViewModel: HomeFeedPlaybackCoordinator.makeDefaultPlayerViewModel(for:))
    }

    func configure(with dramas: [Drama]) async {
        let dramaIDs = dramas.map(\.id)
        let staleIDs = Set(itemsByDramaID.keys).subtracting(dramaIDs)
        staleIDs.forEach { itemsByDramaID.removeValue(forKey: $0) }

        for drama in dramas where itemsByDramaID[drama.id] == nil {
            itemsByDramaID[drama.id] = ItemState(
                drama: drama,
                playerViewModel: makePlayerViewModel(drama)
            )
        }
    }

    func setActiveDrama(id: Drama.ID?) async {
        guard activeDramaID != id else { return }

        let previousActiveID = activeDramaID
        activeDramaID = id

        if let previousActiveID,
           let previousViewModel = itemsByDramaID[previousActiveID]?.playerViewModel {
            previousViewModel.handleDisappear()
        }

        guard let id,
              let currentViewModel = itemsByDramaID[id]?.playerViewModel else {
            return
        }

        await currentViewModel.loadIfNeeded()
    }

    func handleContainerDisappear() {
        guard let activeDramaID,
              let activeViewModel = itemsByDramaID[activeDramaID]?.playerViewModel else {
            return
        }
        activeViewModel.handleDisappear()
    }

    func playbackURL(for dramaID: Drama.ID) -> URL? {
        guard activeDramaID == dramaID else { return nil }
        return itemsByDramaID[dramaID]?.playerViewModel.playbackURL
    }

    func playbackRate(for dramaID: Drama.ID) -> Float {
        guard activeDramaID == dramaID else { return 1.0 }
        return itemsByDramaID[dramaID]?.playerViewModel.playbackRate ?? 1.0
    }

    func updateProgress(_ progress: Double, for dramaID: Drama.ID) {
        guard activeDramaID == dramaID,
              let viewModel = itemsByDramaID[dramaID]?.playerViewModel else { return }
        viewModel.updateCurrentProgress(progress)
    }

    func handlePlaybackEnded(for dramaID: Drama.ID) {
        guard activeDramaID == dramaID,
              let viewModel = itemsByDramaID[dramaID]?.playerViewModel else { return }
        viewModel.handlePlaybackEnded()
    }

    func handlePlaybackFailure(_ message: String, for dramaID: Drama.ID) {
        guard activeDramaID == dramaID,
              let viewModel = itemsByDramaID[dramaID]?.playerViewModel else { return }
        viewModel.handlePlaybackFailure(message: message)
    }

    private static func makeDefaultPlayerViewModel(for drama: Drama) -> PlayerViewModel {
        let repository = PlayerRepository()
        return PlayerViewModel(
            videoId: drama.id,
            fetchPlayerProgressUseCase: FetchPlayerProgressUseCase(repository: repository),
            fetchDramaEpisodesUseCase: FetchDramaEpisodesUseCase(repository: repository),
            startPlaybackUseCase: StartPlaybackUseCase(repository: repository),
            stopPlaybackUseCase: StopPlaybackUseCase(repository: repository),
            playbackSessionStore: KeychainPlaybackSessionStore()
        )
    }
}
