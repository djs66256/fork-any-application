import SwiftUI

struct RankingListView: View {
    let dramas: [RankingDrama]
    let rankingType: RankingType
    let isAppending: Bool
    let appendErrorMessage: String?
    let onTapDrama: (RankingDrama) -> Void
    let onTapBooking: (RankingDrama) -> Void
    let onLoadMore: () -> Void

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: DesignTokens.Spacing.md) {
                    Color.clear
                        .frame(height: 1)
                        .id("ranking-top")

                    ForEach(Array(dramas.enumerated()), id: \.element.id) { index, drama in
                        RankingDramaCardView(
                            drama: drama,
                            rankingType: rankingType,
                            rankIndex: index + 1,
                            onTapCard: { onTapDrama(drama) },
                            onTapBooking: { onTapBooking(drama) }
                        )
                        .onAppear {
                            if index == dramas.count - 1 {
                                onLoadMore()
                            }
                        }
                    }

                    if isAppending {
                        ProgressView("正在加载更多…")
                            .padding(.vertical, DesignTokens.Spacing.md)
                    }

                    if let appendErrorMessage {
                        Text(appendErrorMessage)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.vertical, DesignTokens.Spacing.md)
                    }
                }
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.bottom, DesignTokens.Spacing.lg)
            }
            .onChange(of: dramas.first?.id) { _, _ in
                withAnimation(.easeInOut(duration: 0.2)) {
                    proxy.scrollTo("ranking-top", anchor: .top)
                }
            }
        }
    }
}
