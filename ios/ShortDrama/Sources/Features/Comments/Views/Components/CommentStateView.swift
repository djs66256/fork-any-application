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
            switch state {
            case .idle, .loading:
                VStack(spacing: DesignTokens.Spacing.md) {
                    ProgressView()
                    Text("正在加载评论…")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .empty:
                VStack(spacing: DesignTokens.Spacing.md) {
                    Image(systemName: "message")
                        .font(.system(size: DesignTokens.IconSize.xl))
                        .foregroundStyle(.secondary)
                    Text("暂无评论，快来抢沙发")
                        .font(.headline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .error(let message):
                VStack(spacing: DesignTokens.Spacing.md) {
                    Image(systemName: "wifi.exclamationmark")
                        .font(.system(size: DesignTokens.IconSize.xl))
                        .foregroundStyle(.secondary)
                    Text(message)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                    Button("重试", action: onRetry)
                        .buttonStyle(.borderedProminent)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
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

            if let actionErrorMessage, !actionErrorMessage.isEmpty {
                Text(actionErrorMessage)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding(.horizontal, DesignTokens.Spacing.lg)
                    .padding(.bottom, DesignTokens.Spacing.sm)
            }
        }
    }
}
