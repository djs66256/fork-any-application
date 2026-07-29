import SwiftUI

struct CommentListView: View {
    let comments: [Comment]
    let appendState: CommentSheetViewModel.AppendState
    let likingCommentIDs: Set<String>
    let onLoadMore: () -> Void
    let onRetryAppend: () -> Void
    let onToggleLike: (Comment) -> Void

    var body: some View {
        ScrollView {
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

                    Divider()
                        .padding(.leading, 52)
                }

                footer
            }
        }
    }

    @ViewBuilder
    private var footer: some View {
        switch appendState {
        case .idle:
            EmptyView()
        case .loading:
            ProgressView()
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
                .font(.caption)
                .foregroundStyle(.secondary)
                .padding(DesignTokens.Spacing.lg)
        }
    }
}
