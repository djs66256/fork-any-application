import SwiftUI

struct RankingSecondaryTabBar: View {
    let selected: RankingType
    var showsReferenceDecorations: Bool = false
    let onSelect: (RankingType) -> Void

    private var items: [RankingSecondaryItem] {
        if showsReferenceDecorations {
            return [
                .init(selection: .recommend, title: "推荐榜"),
                .init(selection: .hot, title: "热播榜"),
                .init(selection: nil, title: "臻果榜"),
                .init(selection: .booking, title: "预约榜"),
                .init(selection: nil, title: "新剧榜"),
                .init(selection: nil, title: "热搜榜"),
                .init(selection: nil, title: "分类", showsChevron: true)
            ]
        }

        return [
            .init(selection: .recommend, title: "推荐榜"),
            .init(selection: .hot, title: "热播榜"),
            .init(selection: .booking, title: "预约榜"),
            .init(selection: nil, title: "分类", showsChevron: true)
        ]
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                ForEach(items) { item in
                    if let selection = item.selection {
                        Button {
                            onSelect(selection)
                        } label: {
                            chipLabel(item: item)
                        }
                        .buttonStyle(RankingSecondaryTabButtonStyle())
                    } else {
                        chipLabel(item: item)
                    }
                }
            }
            .padding(.horizontal, 30)
        }
        .scrollIndicators(.hidden)
    }

    private func chipLabel(item: RankingSecondaryItem) -> some View {
        let isSelected = item.selection == selected

        return HStack(spacing: 6) {
            Text(item.title)
                .font(.system(size: 16, weight: .semibold))

            if item.showsChevron {
                Image(systemName: "chevron.down")
                    .font(.system(size: 14, weight: .bold))
            }
        }
        .foregroundStyle(isSelected ? selectedTint : Color.black.opacity(0.62))
        .padding(.horizontal, 20)
        .frame(height: 42)
        .background {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(isSelected ? selectedBackground : unselectedBackground)
        }
    }

    private var selectedTint: Color {
        Color(red: 0.98, green: 0.45, blue: 0.16)
    }

    private var selectedBackground: Color {
        Color(red: 1.0, green: 0.94, blue: 0.88)
    }

    private var unselectedBackground: Color {
        Color(red: 0.96, green: 0.96, blue: 0.96)
    }
}

private struct RankingSecondaryItem: Identifiable {
    let selection: RankingType?
    let title: String
    var showsChevron: Bool = false

    var id: String {
        let base = selection?.rawValue ?? "decorative-\(title)"
        return showsChevron ? "\(base)-chevron" : base
    }
}

private struct RankingSecondaryTabButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.82 : 1)
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}
