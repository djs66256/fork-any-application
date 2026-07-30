import SwiftUI

/// Hot search section for the search discovery page.
struct HotSearchSection: View {
    let state: SearchHomeViewModel.HotSearchState
    let onTapKeyword: (String) -> Void
    let onRetry: () async -> Void

    private var displayItems: [HotSearchItem] {
        switch state {
        case .content(let items) where !items.isEmpty:
            let fallback = Self.placeholderItems
            var merged = items
            for item in fallback where !merged.contains(where: { $0.keyword == item.keyword }) {
                merged.append(item)
            }
            return Array(merged.prefix(6))
        default:
            return Self.placeholderItems
        }
    }

    private var shouldShowFallbackHint: Bool {
        if case .content = state {
            return false
        }
        return true
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .center) {
                Text("猜你想搜")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundStyle(.primary)
                Spacer()
                refreshButton
            }

            if shouldShowFallbackHint {
                Text("为保持发现页版式，当前先展示推荐占位内容")
                    .font(.system(size: 12))
                    .foregroundStyle(Color(uiColor: .systemGray2))
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(Array(displayItems.prefix(3))) { item in
                        Button {
                            onTapKeyword(item.keyword)
                        } label: {
                            HotSearchPosterCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.trailing, 16)
            }

            rankingTabs

            LazyVGrid(
                columns: [
                    GridItem(.flexible(), spacing: 12, alignment: .top),
                    GridItem(.flexible(), spacing: 12, alignment: .top),
                    GridItem(.flexible(), spacing: 12, alignment: .top)
                ],
                alignment: .leading,
                spacing: 18
            ) {
                ForEach(displayItems) { item in
                    Button {
                        onTapKeyword(item.keyword)
                    } label: {
                        RankedHotSearchCard(item: item)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var refreshButton: some View {
        Button {
            Task { await onRetry() }
        } label: {
            Image(systemName: "arrow.clockwise")
                .font(.system(size: 19, weight: .regular))
                .foregroundStyle(.primary)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("刷新猜你想搜")
    }

    private var rankingTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 26) {
                SearchRankingTab(title: "短剧热搜榜", isSelected: true)
                SearchRankingTab(title: "漫剧热搜榜", isSelected: false)
                SearchRankingTab(title: "预约榜", isSelected: false)
                SearchRankingTab(title: "热点话题榜", isSelected: false)
            }
            .padding(.top, 4)
            .padding(.bottom, 8)
            .padding(.trailing, 24)
        }
    }
}

private extension HotSearchSection {
    static let placeholderItems: [HotSearchItem] = [
        HotSearchItem(rank: 1, keyword: "我靠奶奶的金项链", score: 53_600_000),
        HotSearchItem(rank: 2, keyword: "枭雄崛起，从打工开始", score: 48_053_000),
        HotSearchItem(rank: 3, keyword: "一起捉迷藏", score: 51_030_000),
        HotSearchItem(rank: 4, keyword: "回到八零当富翁", score: 26_800_000),
        HotSearchItem(rank: 5, keyword: "千金归来后", score: 24_700_000),
        HotSearchItem(rank: 6, keyword: "神医小娇妻", score: 22_300_000)
    ]
}

private struct SearchRankingTab: View {
    let title: String
    let isSelected: Bool

    var body: some View {
        Text(title)
            .font(.system(size: 16, weight: isSelected ? .semibold : .medium))
            .foregroundStyle(isSelected ? .primary : Color(uiColor: .systemGray2))
            .overlay(alignment: .bottom) {
                if isSelected {
                    Capsule()
                        .fill(Color(red: 0.99, green: 0.84, blue: 0.80))
                        .frame(width: 44, height: 5)
                        .offset(y: 12)
                }
            }
    }
}

private struct HotSearchPosterCard: View {
    let item: HotSearchItem

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(LinearGradient(
                    colors: gradientColors(for: item.rank),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ))
                .frame(width: 148, height: 190)
                .overlay(alignment: .topLeading) {
                    if let badge = posterBadge(for: item.rank) {
                        Text(badge)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 4)
                            .background(Capsule().fill(badgeColor(for: item.rank)))
                            .padding(10)
                    }
                }

            VStack(alignment: .leading, spacing: 6) {
                Text(item.keyword)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(2)
                    .shadow(radius: 4)

                Text("\(formattedScore(item.score))热度")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.white.opacity(0.95))
            }
            .padding(12)
        }
    }

    private func posterBadge(for rank: Int) -> String? {
        switch rank {
        case 1, 2:
            return "新剧"
        case 3:
            return "爆剧"
        default:
            return nil
        }
    }

    private func badgeColor(for rank: Int) -> Color {
        rank == 3 ? Color(red: 1.0, green: 0.42, blue: 0.48) : Color(red: 0.13, green: 0.82, blue: 0.74)
    }

    private func gradientColors(for rank: Int) -> [Color] {
        switch rank {
        case 1:
            return [Color(red: 0.17, green: 0.20, blue: 0.30), Color(red: 0.04, green: 0.05, blue: 0.09)]
        case 2:
            return [Color(red: 0.72, green: 0.56, blue: 0.26), Color(red: 0.24, green: 0.20, blue: 0.13)]
        default:
            return [Color(red: 0.82, green: 0.26, blue: 0.31), Color(red: 0.18, green: 0.06, blue: 0.20)]
        }
    }
}

private struct RankedHotSearchCard: View {
    let item: HotSearchItem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(LinearGradient(
                        colors: tileGradient(for: item.rank),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ))
                    .frame(height: 154)

                Text("\(item.rank)")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(.leading, 10)
                    .padding(.top, 8)

                if item.rank == 1 || item.rank == 2 || item.rank == 5 || item.rank == 6 {
                    Text("新剧")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Capsule().fill(Color(red: 0.13, green: 0.82, blue: 0.74)))
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                        .padding(8)
                }

                Text("\(formattedScore(item.score))热度")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.leading, 10)
                    .padding(.bottom, 10)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
            }

            Text(item.keyword)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(.primary)
                .lineLimit(1)

            Text(detailText(for: item.rank))
                .font(.system(size: 13))
                .foregroundStyle(Color(uiColor: .systemGray2))
                .lineLimit(1)
        }
    }

    private func tileGradient(for rank: Int) -> [Color] {
        switch rank {
        case 1:
            return [Color(red: 0.78, green: 0.45, blue: 0.24), Color(red: 0.20, green: 0.09, blue: 0.10)]
        case 2:
            return [Color(red: 0.16, green: 0.71, blue: 0.77), Color(red: 0.12, green: 0.20, blue: 0.36)]
        case 3:
            return [Color(red: 0.25, green: 0.53, blue: 0.95), Color(red: 0.16, green: 0.16, blue: 0.29)]
        case 4:
            return [Color(red: 0.95, green: 0.84, blue: 0.80), Color(red: 0.72, green: 0.57, blue: 0.55)]
        case 5:
            return [Color(red: 0.93, green: 0.80, blue: 0.85), Color(red: 0.58, green: 0.34, blue: 0.50)]
        default:
            return [Color(red: 0.34, green: 0.68, blue: 0.63), Color(red: 0.14, green: 0.21, blue: 0.28)]
        }
    }

    private func detailText(for rank: Int) -> String {
        switch rank {
        case 1:
            return "796万热搜值"
        case 2:
            return "603万热搜值"
        case 3:
            return "397万热搜值"
        case 4:
            return "352万热搜值"
        case 5:
            return "341万热搜值"
        default:
            return "325万热搜值"
        }
    }
}

private func formattedScore(_ score: Int) -> String {
    if score >= 10_000 {
        let formatted = Double(score) / 10_000.0
        return String(format: formatted >= 100 ? "%.0f万" : "%.1f万", formatted)
    }
    return "\(score)"
}
