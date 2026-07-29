import SwiftUI

struct CommentSheetView: View {
    @StateObject private var viewModel: CommentSheetViewModel
    let onClose: () -> Void
    let onRequireLogin: (CommentLoginContext) -> Void

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
            Divider()
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
            CommentComposerView(
                text: $viewModel.inputText,
                isSubmitting: viewModel.isSubmitting,
                errorMessage: viewModel.composerErrorMessage,
                onSubmit: { Task { await viewModel.submitComment() } }
            )
        }
        .presentationDetents([.medium, .large])
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
            HStack {
                Text("评论 \(viewModel.totalCount)")
                    .font(.headline)
                Spacer()
                Button("关闭", action: onClose)
                    .buttonStyle(.plain)
            }

            Picker("排序", selection: Binding(
                get: { viewModel.selectedSort },
                set: { sort in
                    Task { await viewModel.selectSort(sort) }
                }
            )) {
                ForEach(CommentSort.allCases, id: \.self) { sort in
                    Text(sort.title).tag(sort)
                }
            }
            .pickerStyle(.segmented)
        }
        .padding(DesignTokens.Spacing.lg)
    }
}
