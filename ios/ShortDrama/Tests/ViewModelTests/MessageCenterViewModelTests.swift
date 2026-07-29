import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct MessageCenterViewModelTests {
    private func makeAuthSession() -> AuthSession {
        AuthSession(
            accessToken: "access-token",
            refreshToken: "refresh-token",
            expiresAt: "2026-07-29T12:34:56Z",
            user: AuthUser(
                id: "user-001",
                phone: "13800138000",
                displayName: "测试用户",
                avatarURL: nil,
                role: "viewer",
                isNewUser: false
            )
        )
    }

    @Test("T-05: anonymous message center loads system messages and shows login gate")
    func testAnonymousLoad() async {
        let repository = MockMessageRepository()
        repository.systemMessagesResult = .success(.systemFixture())
        let viewModel = MessageCenterViewModel(
            fetchSystemMessagesUseCase: FetchSystemMessagesUseCase(repository: repository),
            fetchInteractionMessagesUseCase: FetchInteractionMessagesUseCase(repository: repository),
            authTokenProvider: { nil }
        )

        await viewModel.loadInitial()

        #expect(
            viewModel.systemMessages == .content(
                [.fixture()],
                pagination: PagedResult<SystemMessage>.systemFixture().messageCenterPagination
            )
        )
        #expect(viewModel.interactionMessages == .loginRequired)
        #expect(repository.calls == [.fetchSystemMessages(page: 1, pageSize: 20)])
    }

    @Test("T-05: authenticated message center loads both sections")
    func testAuthenticatedLoad() async {
        let repository = MockMessageRepository()
        repository.systemMessagesResult = .success(.systemFixture())
        repository.interactionMessagesResult = .success(.interactionFixture())
        let viewModel = MessageCenterViewModel(
            fetchSystemMessagesUseCase: FetchSystemMessagesUseCase(repository: repository),
            fetchInteractionMessagesUseCase: FetchInteractionMessagesUseCase(repository: repository),
            authTokenProvider: { "access-token" }
        )

        await viewModel.loadInitial()

        #expect(
            viewModel.systemMessages == .content(
                [.fixture()],
                pagination: PagedResult<SystemMessage>.systemFixture().messageCenterPagination
            )
        )
        #expect(
            viewModel.interactionMessages == .content(
                [.fixture()],
                pagination: PagedResult<InteractionMessage>.interactionFixture().messageCenterPagination
            )
        )
        #expect(repository.calls.count == 2)
    }

    @Test("T-05: interaction section error does not block system section")
    func testInteractionErrorIsolation() async {
        let repository = MockMessageRepository()
        repository.systemMessagesResult = .success(.systemFixture())
        repository.interactionMessagesResult = .failure(APIError.server(code: 503, message: "服务暂不可用，请稍后重试"))
        let viewModel = MessageCenterViewModel(
            fetchSystemMessagesUseCase: FetchSystemMessagesUseCase(repository: repository),
            fetchInteractionMessagesUseCase: FetchInteractionMessagesUseCase(repository: repository),
            authTokenProvider: { "access-token" }
        )

        await viewModel.loadInitial()

        #expect(
            viewModel.systemMessages == .content(
                [.fixture()],
                pagination: PagedResult<SystemMessage>.systemFixture().messageCenterPagination
            )
        )
        #expect(viewModel.interactionMessages == .error("服务暂不可用，请稍后重试"))
    }

    @Test("T-05: login success reloads interaction section with fixed context")
    func testHandleLoginSuccessReloadsInteractionSection() async {
        let repository = MockMessageRepository()
        repository.systemMessagesResult = .success(.systemFixture())
        repository.interactionMessagesResult = .success(.interactionFixture(items: []))
        final class TokenBox: @unchecked Sendable {
            var value: String?
        }
        let tokenBox = TokenBox()
        let viewModel = MessageCenterViewModel(
            fetchSystemMessagesUseCase: FetchSystemMessagesUseCase(repository: repository),
            fetchInteractionMessagesUseCase: FetchInteractionMessagesUseCase(repository: repository),
            authTokenProvider: { tokenBox.value }
        )

        await viewModel.loadInitial()
        #expect(viewModel.interactionMessages == .loginRequired)

        tokenBox.value = "access-token"
        repository.interactionMessagesResult = .success(.interactionFixture())
        await viewModel.handleLoginSuccess()

        #expect(viewModel.loginContext == LoginInterceptionContext(source: .messagesEntry, returnRoute: .messages))
        #expect(
            viewModel.interactionMessages == .content(
                [.fixture()],
                pagination: PagedResult<InteractionMessage>.interactionFixture().messageCenterPagination
            )
        )
    }
}
