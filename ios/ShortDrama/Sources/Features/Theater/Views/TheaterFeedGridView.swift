import SwiftUI

struct TheaterFeedGridView: View {
    let dramas: [TheaterDrama]
    let isAppending: Bool
    let appendErrorMessage: String?
    let onTapDrama: (TheaterDrama) -> Void
    let onLoadMore: () -> Void

    private let columns = [
        GridItem(.flexible(), spacing: DesignTokens.Spacing.md),
        GridItem(.flexible(), spacing: DesignTokens.Spacing.md)
    ]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: DesignTokens.Spacing.md) {
                ForEach(Array(dramas.enumerated()), id: \.element.id) { index, drama in
                    TheaterDramaCardView(drama: drama) {
                        onTapDrama(drama)
                    }
                    .onAppear {
                        if index == dramas.count - 1 {
                            onLoadMore()
                        }
                    }
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.top, DesignTokens.Spacing.md)

            footerView
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.md)
        }
    }

    @ViewBuilder
    private var footerView: some View {
        if isAppending {
            ProgressView("正在加载更多…")
        } else if let appendErrorMessage {
            Text(appendErrorMessage)
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }
}

#Preview {
    TheaterFeedGridView(
        dramas: [],
        isAppending: false,
        appendErrorMessage: nil,
        onTapDrama: { _ in },
        onLoadMore: {}
    )
}
