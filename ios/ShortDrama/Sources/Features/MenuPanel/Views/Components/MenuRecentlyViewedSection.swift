import SwiftUI

struct MenuRecentlyViewedSection: View {
    let state: MenuPanelViewModel.RecentlyViewedState
    let isRetrying: Bool
    let onRetry: () async -> Void
    let onTapItem: (RecentlyViewedItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            Text("最近在看")
                .font(.title3)
                .fontWeight(.semibold)

            switch state {
            case .idle, .loading:
                HStack(spacing: DesignTokens.Spacing.md) {
                    ProgressView()
                    Text("正在加载最近在看…")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(DesignTokens.Spacing.lg)
                .background(
                    Color(.secondarySystemBackground),
                    in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                )
            case .content(let items):
                VStack(spacing: DesignTokens.Spacing.md) {
                    ForEach(items.prefix(3)) { item in
                        RecentlyViewedCardView(item: item) {
                            onTapItem(item)
                        }
                    }
                }
            case .empty:
                Text("暂无最近在看，去首页看看吧")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(DesignTokens.Spacing.lg)
                    .background(
                        Color(.secondarySystemBackground),
                        in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                    )
            case .error(let message):
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    Text(message)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Button(isRetrying ? "重试中…" : "重新加载") {
                        Task {
                            await onRetry()
                        }
                    }
                    .buttonStyle(.bordered)
                    .disabled(isRetrying)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(DesignTokens.Spacing.lg)
                .background(
                    Color(.secondarySystemBackground),
                    in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                )
            }
        }
    }
}
