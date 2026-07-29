import Foundation

/// ViewModel for the home screen feed.
@MainActor
final class HomeViewModel: ObservableObject {

    enum ViewState: Equatable {
        case loading
        case content([Drama])
        case empty
        case error(String)
    }

    struct CommentSheetContext: Identifiable, Equatable {
        let id: String
    }

    private enum Constants {
        static let firstPage = 1
        static let pageSize = 10
    }

    // MARK: - Published State

    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isRetrying = false
    @Published private(set) var activeCommentSheet: CommentSheetContext?
    @Published private(set) var pendingCommentLoginContext: CommentLoginContext?

    // MARK: - Dependencies

    private let fetchDramasUseCase: FetchDramasUseCase
    private let fetchDramaCommentsUseCase: FetchDramaCommentsUseCase
    private let createCommentUseCase: CreateCommentUseCase
    private let toggleCommentLikeUseCase: ToggleCommentLikeUseCase
    private let isUserLoggedIn: @Sendable () -> Bool

    // MARK: - State

    private var hasLoaded = false
    private var isRequestInFlight = false

    // MARK: - Init

    init(
        fetchDramasUseCase: FetchDramasUseCase,
        commentRepository: CommentRepositoryProtocol = CommentRepository(),
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false }
    ) {
        self.fetchDramasUseCase = fetchDramasUseCase
        self.fetchDramaCommentsUseCase = FetchDramaCommentsUseCase(repository: commentRepository)
        self.createCommentUseCase = CreateCommentUseCase(repository: commentRepository)
        self.toggleCommentLikeUseCase = ToggleCommentLikeUseCase(repository: commentRepository)
        self.isUserLoggedIn = isUserLoggedIn
    }

    // MARK: - Actions

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        await loadDramas()
    }

    func loadDramas() async {
        await performLoad(isRetry: false)
    }

    func retry() async {
        await performLoad(isRetry: true)
    }

    func openComments(for drama: Drama) {
        openComments(dramaId: drama.id)
    }

    func openComments(dramaId: String) {
        guard !dramaId.isEmpty else { return }
        activeCommentSheet = CommentSheetContext(id: dramaId)
    }

    func closeComments() {
        activeCommentSheet = nil
    }

    func handleCommentLoginRequired(_ context: CommentLoginContext) {
        guard context.source == .home else { return }
        pendingCommentLoginContext = context
        openComments(dramaId: context.dramaId)
    }

    func clearPendingCommentLoginContext() {
        pendingCommentLoginContext = nil
    }

    func restoreCommentContext(_ context: CommentLoginContext) {
        guard context.source == .home else { return }
        openComments(dramaId: context.dramaId)
    }

    func makeCommentSheetViewModel(dramaId: String) -> CommentSheetViewModel {
        CommentSheetViewModel(
            dramaId: dramaId,
            source: .home,
            fetchDramaCommentsUseCase: fetchDramaCommentsUseCase,
            createCommentUseCase: createCommentUseCase,
            toggleCommentLikeUseCase: toggleCommentLikeUseCase,
            isUserLoggedIn: isUserLoggedIn
        )
    }

    // MARK: - Private

    private func performLoad(isRetry: Bool) async {
        guard !isRequestInFlight else { return }

        isRequestInFlight = true
        if isRetry {
            isRetrying = true
        } else {
            viewState = .loading
        }

        defer {
            isRequestInFlight = false
            isRetrying = false
            hasLoaded = true
        }

        do {
            let dramas = try await fetchDramasUseCase.execute(
                page: Constants.firstPage,
                pageSize: Constants.pageSize
            )

            if dramas.isEmpty {
                viewState = .empty
            } else {
                viewState = .content(dramas)
            }
        } catch let error as APIError {
            viewState = .error(error.errorDescription ?? "加载失败，请重试")
        } catch {
            viewState = .error(error.localizedDescription)
        }
    }
}
