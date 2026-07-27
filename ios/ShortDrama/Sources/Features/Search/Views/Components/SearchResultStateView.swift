import SwiftUI

/// Shared state container for search results.
struct SearchResultStateView: View {
    let viewState: SearchResultViewModel.ViewState
    let query: String
    let onPlay: (Drama) -> Void
    let onDetail: (Drama) -> Void
    let onRetry: () async -> Void

    var body: some View {
        switch viewState {
        case .loading:
            VStack(spacing: DesignTokens.Spacing.md) {
                ProgressView()
                Text("正在搜索…")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .content(let dramas):
            ScrollView {
                LazyVStack(spacing: DesignTokens.Spacing.lg) {
                    ForEach(dramas) { drama in
                        HomeDramaCardView(
                            drama: drama,
                            onPlay: { onPlay(drama) },
                            onDetail: { onDetail(drama) }
                        )
                    }
                }
                .padding(.vertical, DesignTokens.Spacing.md)
            }
        case .empty:
            VStack(spacing: DesignTokens.Spacing.md) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: DesignTokens.IconSize.xl))
                    .foregroundStyle(.secondary)
                Text("未找到相关短剧")
                    .font(.headline)
                Text("试试其他关键词：\(query)")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .error(let message):
            VStack(spacing: DesignTokens.Spacing.md) {
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: DesignTokens.IconSize.xl))
                    .foregroundStyle(.secondary)
                Text("搜索失败")
                    .font(.headline)
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                Button("重试") {
                    Task { await onRetry() }
                }
                .buttonStyle(.borderedProminent)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
