import SwiftUI

/// Search history section with clear action.
struct SearchHistorySection: View {
    let items: [SearchHistoryItem]
    let onTapKeyword: (String) -> Void
    let onClear: () -> Void

    private let columns = [
        GridItem(.flexible(minimum: 120), alignment: .leading),
        GridItem(.flexible(minimum: 120), alignment: .leading)
    ]

    private var displayItems: [SearchHistoryItem] {
        let placeholders = [
            "求生",
            "异界",
            "系统",
            "都市日常",
            "我在废土世界种草莓",
            "青春甜宠"
        ].map { SearchHistoryItem(keyword: $0) }

        guard !items.isEmpty else {
            return placeholders
        }

        var merged = items
        for placeholder in placeholders where !merged.contains(where: { $0.keyword == placeholder.keyword }) {
            merged.append(placeholder)
            if merged.count >= 6 {
                break
            }
        }
        return Array(merged.prefix(6))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .center) {
                Text("搜索历史")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.primary)
                Spacer()
                Button {
                    onClear()
                } label: {
                    Image(systemName: "trash")
                        .font(.system(size: 23, weight: .regular))
                        .foregroundStyle(.primary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("清除搜索历史")
            }

            LazyVGrid(columns: columns, alignment: .leading, spacing: 20) {
                ForEach(displayItems) { item in
                    Button {
                        onTapKeyword(item.keyword)
                    } label: {
                        Text(item.keyword)
                            .font(.system(size: 19, weight: .regular))
                            .foregroundStyle(.primary)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}
