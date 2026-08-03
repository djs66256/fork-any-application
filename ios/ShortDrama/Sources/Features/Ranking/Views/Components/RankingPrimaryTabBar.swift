import SwiftUI

struct RankingPrimaryTabBar: View {
    let selected: RankingContentType
    var showsReferenceDecorations: Bool = false
    let onSelect: (RankingContentType) -> Void

    private var items: [RankingPrimaryItem] {
        if showsReferenceDecorations {
            return [
                .init(selection: .all, title: "全部"),
                .init(selection: .liveAction, title: "真人剧"),
                .init(selection: nil, title: "漫剧"),
                .init(selection: .ai, title: "AI剧"),
                .init(selection: nil, title: "演员")
            ]
        }

        return [
            .init(selection: .all, title: "全部"),
            .init(selection: .liveAction, title: "真人剧"),
            .init(selection: .ai, title: "AI剧")
        ]
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: showsReferenceDecorations ? 30 : 28) {
                ForEach(items) { item in
                    if let selection = item.selection {
                        Button {
                            onSelect(selection)
                        } label: {
                            tabLabel(item: item)
                        }
                        .buttonStyle(RankingPrimaryTabButtonStyle())
                    } else {
                        tabLabel(item: item)
                    }
                }
            }
            .padding(.horizontal, 30)
        }
        .scrollIndicators(.hidden)
    }

    private func tabLabel(item: RankingPrimaryItem) -> some View {
        let isSelected = item.selection == selected

        return Text(item.title)
            .font(.system(size: isSelected ? 21 : 20, weight: isSelected ? .bold : .medium))
            .foregroundStyle(isSelected ? Color.black : Color.black.opacity(0.36))
    }
}

private struct RankingPrimaryItem: Identifiable {
    let selection: RankingContentType?
    let title: String

    var id: String {
        selection?.rawValue ?? "decorative-\(title)"
    }
}

private struct RankingPrimaryTabButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.74 : 1)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
