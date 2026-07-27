import SwiftUI

/// Search history section with chips and clear action.
struct SearchHistorySection: View {
    let items: [SearchHistoryItem]
    let onTapKeyword: (String) -> Void
    let onClear: () -> Void

    var body: some View {
        if !items.isEmpty {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
                HStack {
                    Text("搜索历史")
                        .font(.headline)
                    Spacer()
                    Button("清除") {
                        onClear()
                    }
                    .font(.subheadline)
                }

                FlowLayout(items: items, spacing: DesignTokens.Spacing.sm) { item in
                    Button(item.keyword) {
                        onTapKeyword(item.keyword)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
    }
}

private struct FlowLayout<Data: RandomAccessCollection, Content: View>: View where Data.Element: Identifiable {
    let items: Data
    let spacing: CGFloat
    @ViewBuilder let content: (Data.Element) -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: spacing) {
            let rows = Array(items)
            ForEach(rows) { item in
                content(item)
            }
        }
    }
}
