import Foundation
import SwiftUI

@MainActor
final class CommentSheetViewModel: ObservableObject {
    enum ListState: Equatable {
        case idle
        case loading
        case content
        case empty
        case error(String)
    }

    enum AppendState: Equatable {
        case idle
        case loading
        case error(String)
        case noMore
    }

    enum RouteEffect: Equatable {
        case requireLogin(CommentLoginContext)
    }

    private enum Constants {
        static let firstPage = 1
        static let pageSize = 20
        static let maxContentLength = 500
    }

    let dramaId: String
    let source: CommentLoginContext.Source

    @Published private(set) var listState: ListState = .idle
    @Published private(set) var appendState: AppendState = .idle
    @Published private(set) var comments: [Comment] = []
    @Published private(set) var totalCount = 0
    @Published private(set) var selectedSort: CommentSort = .latest
    @Published var inputText = ""
    @Published private(set) var isSubmitting = false
    @Published private(set) var likingCommentIDs: Set<String> = []
    @Published private(set) var routeEffect: RouteEffect?
    @Published private(set) var composerErrorMessage: String?
    @Published private(set) var actionErrorMessage: String?
    @Published private(set) var shouldRestoreOpenSheet = false

    private let fetchDramaCommentsUseCase: FetchDramaCommentsUseCase
    private let createCommentUseCase: CreateCommentUseCase
    private let toggleCommentLikeUseCase: ToggleCommentLikeUseCase
    private let isUserLoggedIn: @Sendable () -> Bool

    private var hasLoaded = false
    private var currentPage = 0
    private var totalPages = 1
    private var requestToken = UUID()

    init(
        dramaId: String,
        source: CommentLoginContext.Source,
        fetchDramaCommentsUseCase: FetchDramaCommentsUseCase,
        createCommentUseCase: CreateCommentUseCase,
        toggleCommentLikeUseCase: ToggleCommentLikeUseCase,
        isUserLoggedIn: @escaping @Sendable () -> Bool = { false }
    ) {
        self.dramaId = dramaId
        self.source = source
        self.fetchDramaCommentsUseCase = fetchDramaCommentsUseCase
        self.createCommentUseCase = createCommentUseCase
        self.toggleCommentLikeUseCase = toggleCommentLikeUseCase
        self.isUserLoggedIn = isUserLoggedIn
    }

    var canSubmit: Bool {
        !normalizedInput.isEmpty && normalizedInput.count <= Constants.maxContentLength && !isSubmitting
    }

    func loadIfNeeded() async {
        guard !hasLoaded else { return }
        hasLoaded = true
        await reloadFirstPage()
    }

    func retry() async {
        await reloadFirstPage()
    }

    func selectSort(_ sort: CommentSort) async {
        guard sort != selectedSort else { return }
        selectedSort = sort
        await reloadFirstPage()
    }

    func loadMoreIfNeeded() async {
        guard !comments.isEmpty,
              appendState != .loading,
              currentPage < totalPages,
              listState != .loading else {
            return
        }

        appendState = .loading
        actionErrorMessage = nil
        let nextPage = currentPage + 1
        let token = requestToken

        do {
            let page = try await fetchDramaCommentsUseCase.execute(
                query: makeQuery(page: nextPage)
            )
            guard token == requestToken else { return }
            currentPage = page.page
            totalPages = max(page.totalPages, page.page)
            totalCount = page.total
            comments.append(contentsOf: page.items)
            appendState = currentPage >= totalPages ? .noMore : .idle
            listState = comments.isEmpty ? .empty : .content
        } catch let error as APIError {
            guard token == requestToken else { return }
            appendState = .error(error.errorDescription ?? "加载更多失败，请稍后重试")
        } catch {
            guard token == requestToken else { return }
            appendState = .error(error.localizedDescription)
        }
    }

    func submitComment() async {
        composerErrorMessage = nil
        actionErrorMessage = nil

        guard validateInput() else { return }
        let content = normalizedInput

        guard isUserLoggedIn() else {
            routeEffect = .requireLogin(
                CommentLoginContext(
                    source: source,
                    dramaId: dramaId,
                    action: PendingCommentAction(kind: .createComment, commentId: nil)
                )
            )
            return
        }

        guard !isSubmitting else { return }
        isSubmitting = true
        defer { isSubmitting = false }

        do {
            let created = try await createCommentUseCase.execute(dramaId: dramaId, content: content)
            comments.insert(created, at: 0)
            totalCount += 1
            listState = .content
            appendState = comments.count >= totalCount ? .noMore : appendState
            inputText = ""
        } catch let error as APIError {
            if case .server(let code, _) = error, code == 401 {
                routeEffect = .requireLogin(
                    CommentLoginContext(
                        source: source,
                        dramaId: dramaId,
                        action: PendingCommentAction(kind: .createComment, commentId: nil)
                    )
                )
            } else {
                actionErrorMessage = error.errorDescription ?? "发表评论失败，请稍后重试"
            }
        } catch {
            actionErrorMessage = error.localizedDescription
        }
    }

    func toggleLike(commentID: String) async {
        guard comments.contains(where: { $0.id == commentID }) else { return }
        actionErrorMessage = nil

        guard isUserLoggedIn() else {
            routeEffect = .requireLogin(
                CommentLoginContext(
                    source: source,
                    dramaId: dramaId,
                    action: PendingCommentAction(kind: .toggleLike, commentId: commentID)
                )
            )
            return
        }

        guard !likingCommentIDs.contains(commentID) else { return }
        likingCommentIDs.insert(commentID)
        defer { likingCommentIDs.remove(commentID) }

        do {
            let result = try await toggleCommentLikeUseCase.execute(dramaId: dramaId, commentId: commentID)
            updateComment(id: commentID) { comment in
                comment.withLikeState(liked: result.liked, likeCount: result.likeCount)
            }
        } catch let error as APIError {
            if case .server(let code, _) = error, code == 401 {
                routeEffect = .requireLogin(
                    CommentLoginContext(
                        source: source,
                        dramaId: dramaId,
                        action: PendingCommentAction(kind: .toggleLike, commentId: commentID)
                    )
                )
            } else {
                actionErrorMessage = error.errorDescription ?? "点赞失败，请稍后重试"
            }
        } catch {
            actionErrorMessage = error.localizedDescription
        }
    }

    func clearRouteEffect() {
        routeEffect = nil
    }

    func restoreLoginContext(_ context: CommentLoginContext) {
        guard context.source == source, context.dramaId == dramaId else { return }
        shouldRestoreOpenSheet = true
    }

    func consumeRestoreOpenSheet() {
        shouldRestoreOpenSheet = false
    }

    private func reloadFirstPage() async {
        requestToken = UUID()
        let token = requestToken
        listState = .loading
        appendState = .idle
        actionErrorMessage = nil
        composerErrorMessage = nil
        currentPage = 0
        totalPages = 1
        comments = []
        totalCount = 0

        do {
            let page = try await fetchDramaCommentsUseCase.execute(
                query: makeQuery(page: Constants.firstPage)
            )
            guard token == requestToken else { return }
            currentPage = page.page
            totalPages = max(page.totalPages, page.page)
            totalCount = page.total
            comments = page.items
            if page.items.isEmpty {
                listState = .empty
                appendState = .noMore
            } else {
                listState = .content
                appendState = currentPage >= totalPages ? .noMore : .idle
            }
        } catch let error as APIError {
            guard token == requestToken else { return }
            listState = .error(error.errorDescription ?? "加载失败，请稍后重试")
        } catch {
            guard token == requestToken else { return }
            listState = .error(error.localizedDescription)
        }
    }

    private func makeQuery(page: Int) -> CommentQuery {
        CommentQuery(dramaId: dramaId, page: page, pageSize: Constants.pageSize, sort: selectedSort)
    }

    private var normalizedInput: String {
        inputText.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private func validateInput() -> Bool {
        let normalized = normalizedInput
        guard !normalized.isEmpty, normalized.count <= Constants.maxContentLength else {
            composerErrorMessage = "请输入 1~500 字评论"
            return false
        }
        return true
    }

    private func updateComment(id: String, transform: (Comment) -> Comment) {
        guard let index = comments.firstIndex(where: { $0.id == id }) else { return }
        comments[index] = transform(comments[index])
    }
}
