import SwiftUI

struct CommentStateView: View {
    let state: CommentSheetViewModel.ListState
    let comments: [Comment]
    let appendState: CommentSheetViewModel.AppendState
    let likingCommentIDs: Set<String>
    let actionErrorMessage: String?
    let onRetry: () -> Void
    let onLoadMore: () -> Void
    let onToggleLike: (Comment) -> Void

    var body: some View {
        VStack(spacing: 0) {
            Group {
                switch state {
                case .idle, .loading:
                    loadingState
                case .empty:
                    emptyState
                case .error(let message):
                    errorState(message: message)
                case .content:
                    CommentListView(
                        comments: comments,
                        appendState: appendState,
                        likingCommentIDs: likingCommentIDs,
                        onLoadMore: onLoadMore,
                        onRetryAppend: onLoadMore,
                        onToggleLike: onToggleLike
                    )
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            if let actionErrorMessage, !actionErrorMessage.isEmpty {
                Text(actionErrorMessage)
                    .font(.system(size: 12))
                    .foregroundStyle(.red)
                    .padding(.horizontal, DesignTokens.Spacing.lg)
                    .padding(.bottom, DesignTokens.Spacing.sm)
            }
        }
        .background(Color.white)
    }

    private var loadingState: some View {
        VStack(spacing: 12) {
            ProgressView()
                .tint(Color.black.opacity(0.6))
            Text("正在加载评论…")
                .font(.system(size: 14))
                .foregroundStyle(Color.black.opacity(0.45))
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "text.bubble")
                .font(.system(size: DesignTokens.IconSize.lg))
                .foregroundStyle(Color.black.opacity(0.25))
            Text("暂无评论，快来抢沙发")
                .font(.system(size: 16, weight: .medium))
                .foregroundStyle(Color.black.opacity(0.58))
        }
    }

    private func errorState(message: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: "wifi.exclamationmark")
                .font(.system(size: DesignTokens.IconSize.lg))
                .foregroundStyle(Color.black.opacity(0.25))
            Text(message)
                .font(.system(size: 14))
                .foregroundStyle(Color.black.opacity(0.52))
                .multilineTextAlignment(.center)
            Button("重试", action: onRetry)
                .buttonStyle(.borderedProminent)
        }
        .padding(.horizontal, DesignTokens.Spacing.xl)
    }
}
