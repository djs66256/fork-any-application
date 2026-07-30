import SwiftUI

struct CommentListView: View {
    let comments: [Comment]
    let appendState: CommentSheetViewModel.AppendState
    let likingCommentIDs: Set<String>
    let onLoadMore: () -> Void
    let onRetryAppend: () -> Void
    let onToggleLike: (Comment) -> Void

    var body: some View {
        ScrollView(showsIndicators: false) {
            LazyVStack(spacing: 0) {
                ForEach(Array(comments.enumerated()), id: \.element.id) { index, comment in
                    CommentRowView(
                        comment: comment,
                        isLiking: likingCommentIDs.contains(comment.id),
                        onToggleLike: { onToggleLike(comment) }
                    )
                    .onAppear {
                        if index == comments.count - 1 {
                            onLoadMore()
                        }
                    }
                }

                footer
            }
            .padding(.top, 4)
            .padding(.bottom, 8)
        }
        .background(Color.white)
    }

    @ViewBuilder
    private var footer: some View {
        switch appendState {
        case .idle:
            EmptyView()
        case .loading:
            ProgressView()
                .tint(Color.black.opacity(0.45))
                .padding(DesignTokens.Spacing.lg)
        case .error(let message):
            VStack(spacing: DesignTokens.Spacing.sm) {
                Text(message)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button("重试更多", action: onRetryAppend)
                    .buttonStyle(.bordered)
            }
            .padding(DesignTokens.Spacing.lg)
        case .noMore:
            Text("没有更多评论了")
                .font(.system(size: 12))
                .foregroundStyle(Color.black.opacity(0.28))
                .padding(.vertical, DesignTokens.Spacing.lg)
        }
    }
}
