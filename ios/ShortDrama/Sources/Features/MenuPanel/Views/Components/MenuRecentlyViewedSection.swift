import SwiftUI

struct MenuRecentlyViewedSection: View {
    let state: MenuPanelViewModel.RecentlyViewedState
    let isRetrying: Bool
    let onRetry: () async -> Void
    let onTapItem: (RecentlyViewedItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("最近在看")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(MenuPanelStyle.title)
                .padding(.horizontal, 4)

            switch state {
            case .idle, .loading:
                loadingCard
            case .content(let items):
                contentCard(items: Array(items.prefix(3)))
            case .empty, .error:
                fallbackContentCard
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var loadingCard: some View {
        HStack(spacing: 10) {
            ProgressView()
            Text("正在加载最近在看…")
                .font(.system(size: 14))
                .foregroundStyle(MenuPanelStyle.secondaryText)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, 20)
        .background(sectionBackground)
    }

    private func contentCard(items: [RecentlyViewedItem]) -> some View {
        HStack(alignment: .top, spacing: 10) {
            ForEach(items) { item in
                RecentlyViewedCardView(item: item) {
                    onTapItem(item)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.top, 16)
        .padding(.bottom, 18)
        .background(sectionBackground)
    }

    private var fallbackContentCard: some View {
        HStack(alignment: .top, spacing: 10) {
            ForEach(fallbackItems) { item in
                RecentlyViewedCardView(item: item) {
                    onTapItem(item)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.top, 16)
        .padding(.bottom, 18)
        .background(sectionBackground)
    }

    private var sectionBackground: some View {
        RoundedRectangle(cornerRadius: 18, style: .continuous)
            .fill(MenuPanelStyle.cardBackground)
            .overlay {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(MenuPanelStyle.cardBorder, lineWidth: 1)
            }
    }

    private var fallbackItems: [RecentlyViewedItem] {
        [
            RecentlyViewedItem(
                dramaId: "fallback-village",
                title: "村里的吃人鬼",
                coverURL: nil,
                episodeNumber: 1,
                progress: 0.2,
                updatedAt: ""
            ),
            RecentlyViewedItem(
                dramaId: "fallback-sword",
                title: "一剑挽仙洲",
                coverURL: nil,
                episodeNumber: 119,
                progress: 0.55,
                updatedAt: ""
            ),
            RecentlyViewedItem(
                dramaId: "fallback-emperor",
                title: "兼职帝君",
                coverURL: nil,
                episodeNumber: 1,
                progress: 0.08,
                updatedAt: ""
            )
        ]
    }
}
