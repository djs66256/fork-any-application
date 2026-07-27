import SwiftUI

/// Hot search section for the search discovery page.
struct HotSearchSection: View {
    let state: SearchHomeViewModel.HotSearchState
    let onTapKeyword: (String) -> Void
    let onRetry: () async -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            Text("热搜榜")
                .font(.headline)

            switch state {
            case .idle, .loading:
                HStack(spacing: DesignTokens.Spacing.sm) {
                    ProgressView()
                    Text("热搜加载中…")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            case .content(let items):
                ForEach(items) { item in
                    Button {
                        onTapKeyword(item.keyword)
                    } label: {
                        HStack(spacing: DesignTokens.Spacing.md) {
                            Text("\(item.rank)")
                                .font(.headline)
                                .frame(width: 24, alignment: .leading)
                            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                                Text(item.keyword)
                                    .font(.body)
                                    .foregroundStyle(.primary)
                                Text("热度 \(item.score)")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                        .padding(.vertical, DesignTokens.Spacing.sm)
                    }
                    .buttonStyle(.plain)

                    if item.id != items.last?.id {
                        Divider()
                    }
                }
            case .error(let message):
                VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    Text(message)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    Button("重试") {
                        Task { await onRetry() }
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
    }
}
