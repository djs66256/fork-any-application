import SwiftUI

struct CommentSheetView: View {
    @StateObject private var viewModel: CommentSheetViewModel
    let onClose: () -> Void
    let onRequireLogin: (CommentLoginContext) -> Void

    private let isPreviewAlignmentMode = ProcessInfo.processInfo.arguments.contains("--preview-player-comments-sheet")

    init(
        viewModel: CommentSheetViewModel,
        onClose: @escaping () -> Void = {},
        onRequireLogin: @escaping (CommentLoginContext) -> Void = { _ in }
    ) {
        _viewModel = StateObject(wrappedValue: viewModel)
        self.onClose = onClose
        self.onRequireLogin = onRequireLogin
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            hotTopicRow
            CommentStateView(
                state: viewModel.listState,
                comments: viewModel.comments,
                appendState: viewModel.appendState,
                likingCommentIDs: viewModel.likingCommentIDs,
                actionErrorMessage: viewModel.actionErrorMessage,
                onRetry: { Task { await viewModel.retry() } },
                onLoadMore: { Task { await viewModel.loadMoreIfNeeded() } },
                onToggleLike: { comment in
                    Task { await viewModel.toggleLike(commentID: comment.id) }
                }
            )
        }
        .background(Color.white.opacity(0.98))
        .safeAreaInset(edge: .bottom, spacing: 0) {
            CommentComposerView(
                text: $viewModel.inputText,
                isSubmitting: viewModel.isSubmitting,
                errorMessage: viewModel.composerErrorMessage,
                onSubmit: { Task { await viewModel.submitComment() } }
            )
        }
        .presentationDetents([.fraction(0.68), .large])
        .presentationDragIndicator(.hidden)
        .presentationCornerRadius(28)
        .presentationBackground(Color.white)
        .task {
            await viewModel.loadIfNeeded()
        }
        .onReceive(viewModel.$routeEffect) { effect in
            guard let effect else { return }
            switch effect {
            case .requireLogin(let context):
                onRequireLogin(context)
            }
            viewModel.clearRouteEffect()
        }
    }

    private var header: some View {
        VStack(spacing: DesignTokens.Spacing.md) {
            Capsule()
                .fill(Color.black.opacity(0.14))
                .frame(width: 42, height: 5)
                .padding(.top, DesignTokens.Spacing.sm)

            ZStack {
                Text("\(viewModel.totalCount)条评论")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Color.black)

                HStack {
                    Button(action: onClose) {
                        Image(systemName: "chevron.down")
                            .font(.system(size: 22, weight: .semibold))
                            .foregroundStyle(Color.black.opacity(0.82))
                            .frame(width: 36, height: 36)
                    }
                    .buttonStyle(.plain)

                    Spacer()
                }
            }
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.bottom, DesignTokens.Spacing.md)
    }

    private var hotTopicRow: some View {
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text("大家都在搜：")
                .font(.system(size: 14))
                .foregroundStyle(Color.black.opacity(0.72))

            Text(isPreviewAlignmentMode ? "都重生了，谁还装富二代啊第三季" : "剧情讨论、角色关系")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Color.blue)
                .lineLimit(1)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.vertical, 14)
        .background(Color.black.opacity(0.02))
    }
}
