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
            ScrollView(showsIndicators: false) {
                LazyVStack(spacing: 0) {
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
                        HStack(spacing: DesignTokens.Spacing.sm) {
                            ProgressView()
                                .controlSize(.small)
                            Text("正在加载更多…")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 18)
                    }

                    if let appendErrorMessage {
                        Text(appendErrorMessage)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.vertical, 18)
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 10)
                .padding(.bottom, DesignTokens.Spacing.xl)
            }
            .padding(.bottom, DesignTokens.Spacing.xl)
            .onChange(of: dramas.first?.id) { _, _ in
                withAnimation(.easeInOut(duration: 0.2)) {
                    proxy.scrollTo("ranking-top", anchor: .top)
                }
            }
        }
    }
}
