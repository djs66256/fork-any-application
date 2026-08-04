import Foundation

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

    struct CheckInPopupState: Equatable {
        let serverDate: String
        let rewardCopy: String
        let days: [SignInDay]
        let todaySigned: Bool
        let isSubmitting: Bool
        let feedbackMessage: String?
        let isError: Bool

        var buttonTitle: String {
            if isSubmitting {
                return "签到中…"
            }
            return todaySigned ? "今日已签到" : "立即签到"
        }

        var isSubmitDisabled: Bool {
            todaySigned || isSubmitting
        }
    }

    private enum Constants {
        static let firstPage = 1
        static let pageSize = 10
        static let featuredDramaPopupAutoHideDuration: Duration = .seconds(3)
    }

    @Published private(set) var viewState: ViewState = .loading
    @Published private(set) var isRetrying = false
    @Published private(set) var activeCommentSheet: CommentSheetContext?
    @Published private(set) var pendingCommentLoginContext: CommentLoginContext?
    @Published private(set) var checkInPopupState: CheckInPopupState?
    @Published private(set) var isPresentingBlockingOverlay = false
    @Published private(set) var isFeaturedDramaPopupVisible = false

    private let fetchDramasUseCase: FetchDramasUseCase
    private let fetchDramaCommentsUseCase: FetchDramaCommentsUseCase
    private let createCommentUseCase: CreateCommentUseCase
    private let toggleCommentLikeUseCase: ToggleCommentLikeUseCase
    private let fetchCheckInStatusUseCase: FetchCheckInStatusUseCase
    private let submitCheckInUseCase: SubmitCheckInUseCase
    private let installationIdStore: InstallationIdStore
    private let dismissStore: CheckInPopupDismissStore
    private let isUserLoggedIn: @Sendable () -> Bool
    private let accessTokenProvider: @Sendable () -> String?
    private let featuredDramaPopupAutoHideDuration: Duration
    private let featuredDramaPopupSleep: @Sendable (Duration) async throws -> Void

    private var hasLoaded = false
    private var isRequestInFlight = false
    private var hasEvaluatedCheckInPopup = false
    private var hasPresentedFeaturedDramaPopup = false
    private var featuredDramaPopupHideTask: Task<Void, Never>?

    init(
        fetchDramasUseCase: FetchDramasUseCase,
        commentRepository: CommentRepositoryProtocol = CommentRepository(),
        fetchCheckInStatusUseCase: FetchCheckInStatusUseCase = .init(
            repository: CheckInRepository()
        ),
        submitCheckInUseCase: SubmitCheckInUseCase = .init(
            repository: CheckInRepository()
        ),
        installationIdStore: InstallationIdStore = KeychainInstallationIdStore(),
        dismissStore: CheckInPopupDismissStore = UserDefaultsCheckInPopupDismissStore(),
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false },
        accessTokenProvider: @escaping @Sendable () -> String? = { nil },
        featuredDramaPopupAutoHideDuration: Duration = Constants.featuredDramaPopupAutoHideDuration,
        featuredDramaPopupSleep: @escaping @Sendable (Duration) async throws -> Void = { duration in
            try await Task.sleep(for: duration)
        }
    ) {
        self.fetchDramasUseCase = fetchDramasUseCase
        self.fetchDramaCommentsUseCase = FetchDramaCommentsUseCase(repository: commentRepository)
        self.createCommentUseCase = CreateCommentUseCase(repository: commentRepository)
        self.toggleCommentLikeUseCase = ToggleCommentLikeUseCase(repository: commentRepository)
        self.fetchCheckInStatusUseCase = fetchCheckInStatusUseCase
        self.submitCheckInUseCase = submitCheckInUseCase
        self.installationIdStore = installationIdStore
        self.dismissStore = dismissStore
        self.isUserLoggedIn = isUserLoggedIn
        self.accessTokenProvider = accessTokenProvider
        self.featuredDramaPopupAutoHideDuration = featuredDramaPopupAutoHideDuration
        self.featuredDramaPopupSleep = featuredDramaPopupSleep
    }

    deinit {
        featuredDramaPopupHideTask?.cancel()
    }

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

    func updateAuthState(isUserLoggedIn: Bool, accessToken: String?) {
        let isBlockingOverlayActive = isUserLoggedIn && accessToken == nil
        isPresentingBlockingOverlay = isBlockingOverlayActive
        if isBlockingOverlayActive {
            checkInPopupState = nil
        }
    }

    func dismissCheckInPopup() {
        guard let serverDate = checkInPopupState?.serverDate else {
            checkInPopupState = nil
            return
        }
        dismissStore.markDismissed(serverDate: serverDate)
        checkInPopupState = nil
    }

    func submitCheckIn() async {
        guard let popupState = checkInPopupState, !popupState.todaySigned, !popupState.isSubmitting else { return }

        checkInPopupState = CheckInPopupState(
            serverDate: popupState.serverDate,
            rewardCopy: popupState.rewardCopy,
            days: popupState.days,
            todaySigned: popupState.todaySigned,
            isSubmitting: true,
            feedbackMessage: nil,
            isError: false
        )

        do {
            let accessToken = accessTokenProvider()?.trimmingCharacters(in: .whitespacesAndNewlines)
            let installationId = try installationIdForCheckInRequest(accessToken: accessToken)
            let status = try await submitCheckInUseCase.execute(
                installationId: installationId,
                accessToken: accessToken
            )
            dismissStore.markDismissed(serverDate: status.serverDate)
            checkInPopupState = makePopupState(from: status, feedbackMessage: "签到成功", isError: false)
        } catch let error as APIError {
            checkInPopupState = CheckInPopupState(
                serverDate: popupState.serverDate,
                rewardCopy: popupState.rewardCopy,
                days: popupState.days,
                todaySigned: popupState.todaySigned,
                isSubmitting: false,
                feedbackMessage: error.errorDescription ?? "签到失败，请重试",
                isError: true
            )
        } catch {
            checkInPopupState = CheckInPopupState(
                serverDate: popupState.serverDate,
                rewardCopy: popupState.rewardCopy,
                days: popupState.days,
                todaySigned: popupState.todaySigned,
                isSubmitting: false,
                feedbackMessage: "签到失败，请重试",
                isError: true
            )
        }
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
            let dramas = try await fetchDramasUseCase.execute(page: Constants.firstPage, pageSize: Constants.pageSize)

            if dramas.isEmpty {
                viewState = .empty
            } else {
                viewState = .content(dramas)
                presentFeaturedDramaPopupIfNeeded()
            }

            await evaluateCheckInPopupIfNeeded()
        } catch let error as APIError {
            viewState = .error(error.errorDescription ?? "加载失败，请重试")
        } catch {
            viewState = .error(error.localizedDescription)
        }
    }

    private func evaluateCheckInPopupIfNeeded() async {
        guard !hasEvaluatedCheckInPopup else { return }
        hasEvaluatedCheckInPopup = true
        guard activeCommentSheet == nil,
              pendingCommentLoginContext == nil,
              !isPresentingBlockingOverlay else { return }

        do {
            let accessToken = accessTokenProvider()?.trimmingCharacters(in: .whitespacesAndNewlines)
            let installationId = try installationIdForCheckInRequest(accessToken: accessToken)
            let status = try await fetchCheckInStatusUseCase.execute(
                installationId: installationId,
                accessToken: accessToken
            )
            guard status.shouldShowPopup,
                  !status.todaySigned,
                  !dismissStore.isDismissed(serverDate: status.serverDate) else {
                checkInPopupState = nil
                return
            }
            checkInPopupState = makePopupState(from: status)
        } catch {
            checkInPopupState = nil
        }
    }

    private func installationIdForCheckInRequest(accessToken: String?) throws -> String? {
        if let accessToken, !accessToken.isEmpty {
            return try? installationIdStore.getOrCreateInstallationId()
        }
        return try installationIdStore.getOrCreateInstallationId()
    }

    private func presentFeaturedDramaPopupIfNeeded() {
        guard !hasPresentedFeaturedDramaPopup else { return }

        hasPresentedFeaturedDramaPopup = true
        isFeaturedDramaPopupVisible = true
        featuredDramaPopupHideTask?.cancel()

        let autoHideDuration = featuredDramaPopupAutoHideDuration
        let sleep = featuredDramaPopupSleep
        featuredDramaPopupHideTask = Task {
            do {
                try await sleep(autoHideDuration)
            } catch {
                return
            }

            guard !Task.isCancelled else { return }

            await MainActor.run { [weak self] in
                self?.isFeaturedDramaPopupVisible = false
                self?.featuredDramaPopupHideTask = nil
            }
        }
    }

    private func makePopupState(
        from status: SignInStatus,
        feedbackMessage: String? = nil,
        isError: Bool = false
    ) -> CheckInPopupState {
        CheckInPopupState(
            serverDate: status.serverDate,
            rewardCopy: status.rewardCopy,
            days: status.days,
            todaySigned: status.todaySigned,
            isSubmitting: false,
            feedbackMessage: feedbackMessage,
            isError: isError
        )
    }
}
