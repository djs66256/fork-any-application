import Foundation

@MainActor
final class MessageCenterViewModel: ObservableObject {
    private enum Constants {
        static let firstPage = 1
        static let pageSize = 20
    }

    struct SectionPagination: Equatable {
        let page: Int
        let pageSize: Int
        let total: Int
        let totalPages: Int
    }

    enum SectionState<Item: Equatable & Sendable>: Equatable {
        case idle
        case loading
        case content([Item], pagination: SectionPagination)
        case empty
        case error(String)
        case loginRequired
    }

    @Published private(set) var systemMessages: SectionState<SystemMessage> = .idle
    @Published private(set) var interactionMessages: SectionState<InteractionMessage> = .idle

    let loginContext = LoginInterceptionContext(source: .messagesEntry, returnRoute: .messages)

    private let fetchSystemMessagesUseCase: FetchSystemMessagesUseCase
    private let fetchInteractionMessagesUseCase: FetchInteractionMessagesUseCase
    private let authTokenProvider: @Sendable () -> String?

    init(
        fetchSystemMessagesUseCase: FetchSystemMessagesUseCase,
        fetchInteractionMessagesUseCase: FetchInteractionMessagesUseCase,
        authTokenProvider: @escaping @Sendable () -> String?
    ) {
        self.fetchSystemMessagesUseCase = fetchSystemMessagesUseCase
        self.fetchInteractionMessagesUseCase = fetchInteractionMessagesUseCase
        self.authTokenProvider = authTokenProvider
    }

    func loadInitial() async {
        await loadSystemMessages()
        await loadInteractionMessagesIfNeeded()
    }

    func retrySystemMessages() async {
        await loadSystemMessages()
    }

    func retryInteractionMessages() async {
        await loadInteractionMessagesIfNeeded(force: true)
    }

    func handleLoginSuccess() async {
        await loadInteractionMessagesIfNeeded(force: true)
    }

    private func loadSystemMessages() async {
        systemMessages = .loading

        do {
            let result = try await fetchSystemMessagesUseCase.execute(page: Constants.firstPage, pageSize: Constants.pageSize)
            if result.items.isEmpty {
                systemMessages = .empty
            } else {
                systemMessages = .content(result.items, pagination: result.messageCenterPagination)
            }
        } catch let error as APIError {
            systemMessages = .error(error.errorDescription ?? "加载失败，请稍后重试")
        } catch {
            systemMessages = .error(error.localizedDescription)
        }
    }

    private func loadInteractionMessagesIfNeeded(force: Bool = false) async {
        guard let accessToken = authTokenProvider(), !accessToken.isEmpty else {
            interactionMessages = .loginRequired
            return
        }

        if !force, case .content = interactionMessages {
            return
        }

        interactionMessages = .loading

        do {
            let result = try await fetchInteractionMessagesUseCase.execute(
                page: Constants.firstPage,
                pageSize: Constants.pageSize,
                accessToken: accessToken
            )
            if result.items.isEmpty {
                interactionMessages = .empty
            } else {
                interactionMessages = .content(result.items, pagination: result.messageCenterPagination)
            }
        } catch let error as APIError {
            if error.statusCode == 401 || error.businessCode == "AUTH_UNAUTHORIZED" {
                interactionMessages = .loginRequired
            } else {
                interactionMessages = .error(error.errorDescription ?? "加载失败，请稍后重试")
            }
        } catch {
            interactionMessages = .error(error.localizedDescription)
        }
    }
}

extension PagedResult {
    var messageCenterPagination: MessageCenterViewModel.SectionPagination {
        MessageCenterViewModel.SectionPagination(page: page, pageSize: pageSize, total: total, totalPages: totalPages)
    }
}
