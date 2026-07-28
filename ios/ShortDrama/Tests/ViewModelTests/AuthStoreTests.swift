import Foundation
import Testing
@testable import ShortDrama

@MainActor
struct AuthStoreTests {
    private func makeSession(phone: String = "13800138000") -> AuthSession {
        AuthSession(
            accessToken: "access-token",
            refreshToken: "refresh-token",
            expiresAt: "2026-07-28T12:34:56Z",
            user: AuthUser(
                id: "user-001",
                phone: phone,
                displayName: "测试用户",
                avatarURL: nil,
                role: "viewer",
                isNewUser: false
            )
        )
    }

    @Test("auth store stays anonymous when no persisted session exists")
    func testRestoreWithoutPersistedSession() async {
        let sessionStore = MockAuthSessionStore()
        let repository = MockAuthRepository()
        let store = AuthStore(
            sessionStore: sessionStore,
            getCurrentUserUseCase: GetCurrentUserUseCase(repository: repository),
            refreshSessionUseCase: RefreshSessionUseCase(repository: repository),
            logoutUseCase: LogoutUseCase(repository: repository)
        )

        await store.restoreIfNeeded()

        #expect(store.status == .anonymous)
        #expect(store.currentUser == nil)
        #expect(sessionStore.loadCallCount == 1)
        #expect(repository.getCurrentUserCallCount == 0)
    }

    @Test("auth store restores persisted session and refreshes current user")
    func testRestoreSessionUpdatesCurrentUser() async {
        let sessionStore = MockAuthSessionStore()
        sessionStore.loadedSession = makeSession(phone: "13800138000")
        let repository = MockAuthRepository()
        repository.getCurrentUserResult = .success(
            AuthUser(
                id: "user-001",
                phone: "13900139000",
                displayName: "已恢复用户",
                avatarURL: nil,
                role: "viewer",
                isNewUser: false
            )
        )
        let store = AuthStore(
            sessionStore: sessionStore,
            getCurrentUserUseCase: GetCurrentUserUseCase(repository: repository),
            refreshSessionUseCase: RefreshSessionUseCase(repository: repository),
            logoutUseCase: LogoutUseCase(repository: repository)
        )

        await store.restoreIfNeeded()

        guard case .authenticated(let session) = store.status else {
            Issue.record("Expected authenticated status after restore")
            return
        }
        #expect(session.user.phone == "13900139000")
        #expect(store.currentUser?.displayName == "已恢复用户")
        #expect(repository.getCurrentUserCallCount == 1)
        #expect(sessionStore.saveCallCount == 1)
    }

    @Test("auth store refreshes session when access token is unauthorized")
    func testRestoreRefreshesUnauthorizedSession() async {
        let sessionStore = MockAuthSessionStore()
        sessionStore.loadedSession = makeSession()
        let repository = MockAuthRepository()
        repository.getCurrentUserResult = .failure(
            APIError.business(statusCode: 401, businessCode: "AUTH_UNAUTHORIZED", message: "登录已失效")
        )
        repository.refreshSessionResult = .success(
            AuthSession(
                accessToken: "new-access-token",
                refreshToken: "new-refresh-token",
                expiresAt: "2026-07-28T13:34:56Z",
                user: AuthUser(
                    id: "user-001",
                    phone: "13800138000",
                    displayName: "刷新后用户",
                    avatarURL: nil,
                    role: "viewer",
                    isNewUser: false
                )
            )
        )
        let store = AuthStore(
            sessionStore: sessionStore,
            getCurrentUserUseCase: GetCurrentUserUseCase(repository: repository),
            refreshSessionUseCase: RefreshSessionUseCase(repository: repository),
            logoutUseCase: LogoutUseCase(repository: repository)
        )

        await store.restoreIfNeeded()

        guard case .authenticated(let session) = store.status else {
            Issue.record("Expected authenticated status after refresh")
            return
        }
        #expect(session.accessToken == "new-access-token")
        #expect(repository.refreshSessionCallCount == 1)
        #expect(sessionStore.lastSavedSession?.refreshToken == "new-refresh-token")
    }

    @Test("auth store marks session expired when refresh fails")
    func testRestoreExpiresWhenRefreshFails() async {
        let sessionStore = MockAuthSessionStore()
        sessionStore.loadedSession = makeSession()
        let repository = MockAuthRepository()
        repository.getCurrentUserResult = .failure(
            APIError.business(statusCode: 401, businessCode: "AUTH_UNAUTHORIZED", message: "登录已失效")
        )
        repository.refreshSessionResult = .failure(
            APIError.business(statusCode: 401, businessCode: "AUTH_REFRESH_EXPIRED", message: "请重新登录")
        )
        let store = AuthStore(
            sessionStore: sessionStore,
            getCurrentUserUseCase: GetCurrentUserUseCase(repository: repository),
            refreshSessionUseCase: RefreshSessionUseCase(repository: repository),
            logoutUseCase: LogoutUseCase(repository: repository)
        )

        await store.restoreIfNeeded()

        #expect(store.status == .expired)
        #expect(store.currentUser == nil)
        #expect(sessionStore.clearCallCount == 1)
    }

    @Test("auth store persists session on login success")
    func testHandleLoginSuccessPersistsSession() async throws {
        let sessionStore = MockAuthSessionStore()
        let repository = MockAuthRepository()
        let session = makeSession()
        let store = AuthStore(
            sessionStore: sessionStore,
            getCurrentUserUseCase: GetCurrentUserUseCase(repository: repository),
            refreshSessionUseCase: RefreshSessionUseCase(repository: repository),
            logoutUseCase: LogoutUseCase(repository: repository)
        )

        try await store.handleLoginSuccess(session)

        #expect(store.status == .authenticated(session))
        #expect(store.currentUser == session.user)
        #expect(sessionStore.lastSavedSession == session)
    }

    @Test("auth store clears persisted session on logout")
    func testLogoutClearsPersistedSession() async throws {
        let sessionStore = MockAuthSessionStore()
        let repository = MockAuthRepository()
        let session = makeSession()
        let store = AuthStore(
            sessionStore: sessionStore,
            getCurrentUserUseCase: GetCurrentUserUseCase(repository: repository),
            refreshSessionUseCase: RefreshSessionUseCase(repository: repository),
            logoutUseCase: LogoutUseCase(repository: repository)
        )
        try await store.handleLoginSuccess(session)

        try await store.logout()

        #expect(store.status == .anonymous)
        #expect(store.currentUser == nil)
        #expect(sessionStore.clearCallCount == 1)
        #expect(repository.lastLogoutAccessToken == "access-token")
    }
}
