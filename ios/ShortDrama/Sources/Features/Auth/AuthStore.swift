import Foundation

@MainActor
final class AuthStore: ObservableObject {
    @Published private(set) var status: AuthStatus = .anonymous
    @Published private(set) var currentUser: AuthUser?

    private let sessionStore: AuthSessionStore
    private let getCurrentUserUseCase: GetCurrentUserUseCase
    private let refreshSessionUseCase: RefreshSessionUseCase
    private let logoutUseCase: LogoutUseCase

    private var hasAttemptedRestore = false

    init(
        sessionStore: AuthSessionStore = KeychainAuthSessionStore(),
        getCurrentUserUseCase: GetCurrentUserUseCase = GetCurrentUserUseCase(repository: AuthRepository()),
        refreshSessionUseCase: RefreshSessionUseCase = RefreshSessionUseCase(repository: AuthRepository()),
        logoutUseCase: LogoutUseCase = LogoutUseCase(repository: AuthRepository())
    ) {
        self.sessionStore = sessionStore
        self.getCurrentUserUseCase = getCurrentUserUseCase
        self.refreshSessionUseCase = refreshSessionUseCase
        self.logoutUseCase = logoutUseCase
    }

    var isAuthenticated: Bool {
        switch status {
        case .authenticated, .refreshing:
            return true
        case .anonymous, .restoring, .expired:
            return false
        }
    }

    var accessToken: String? {
        status.currentSession?.accessToken
    }

    var accessTokenExpiresAt: String? {
        status.currentSession?.expiresAt
    }

    func restoreIfNeeded() async {
        guard !hasAttemptedRestore else { return }
        hasAttemptedRestore = true

        do {
            guard let session = try sessionStore.load() else {
                applyAnonymousState()
                return
            }

            status = .restoring
            currentUser = session.user

            do {
                let user = try await getCurrentUserUseCase.execute(accessToken: session.accessToken)
                let restoredSession = AuthSession(
                    accessToken: session.accessToken,
                    refreshToken: session.refreshToken,
                    expiresAt: session.expiresAt,
                    user: user
                )
                try sessionStore.save(restoredSession)
                applyAuthenticatedState(restoredSession)
            } catch let error as APIError where shouldRefreshSession(for: error) {
                await refreshStoredSession(session)
            } catch {
                applyAuthenticatedState(session)
            }
        } catch {
            applyAnonymousState()
        }
    }

    func handleLoginSuccess(_ session: AuthSession) async throws {
        try sessionStore.save(session)
        applyAuthenticatedState(session)
    }

    func logout() async throws {
        try await logoutUseCase.execute(accessToken: status.currentSession?.accessToken)
        try sessionStore.clear()
        applyAnonymousState()
    }

    private func refreshStoredSession(_ session: AuthSession) async {
        status = .refreshing(session)
        currentUser = session.user

        do {
            let refreshedSession = try await refreshSessionUseCase.execute(refreshToken: session.refreshToken)
            try sessionStore.save(refreshedSession)
            applyAuthenticatedState(refreshedSession)
        } catch {
            try? sessionStore.clear()
            currentUser = nil
            status = .expired
        }
    }

    private func shouldRefreshSession(for error: APIError) -> Bool {
        error.statusCode == 401
            || error.businessCode == "AUTH_UNAUTHORIZED"
            || error.businessCode == "AUTH_ACCESS_TOKEN_EXPIRED"
    }

    private func applyAuthenticatedState(_ session: AuthSession) {
        currentUser = session.user
        status = .authenticated(session)
    }

    private func applyAnonymousState() {
        currentUser = nil
        status = .anonymous
    }
}
