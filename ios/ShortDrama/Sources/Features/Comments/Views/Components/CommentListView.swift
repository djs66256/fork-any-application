import SwiftUI

struct CommentListView: View {
    private let sheetBackground = Color(red: 0.975, green: 0.975, blue: 0.975)

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
            .padding(.top, 6)
            .padding(.bottom, 10)
        }
        .background(sheetBackground)
    }

    @ViewBuilder
    private var footer: some View {
        switch appendState {
        case .idle, .noMore:
            EmptyView()
        case .loading:
            ProgressView()
                .tint(Color.black.opacity(0.4))
                .padding(.vertical, DesignTokens.Spacing.lg)
        case .error(let message):
            VStack(spacing: DesignTokens.Spacing.sm) {
                Text(message)
                    .font(.system(size: 12))
                    .foregroundStyle(Color.black.opacity(0.42))
                Button("重试更多", action: onRetryAppend)
                    .buttonStyle(.bordered)
            }
            .padding(.vertical, DesignTokens.Spacing.lg)
        }
    }
}
