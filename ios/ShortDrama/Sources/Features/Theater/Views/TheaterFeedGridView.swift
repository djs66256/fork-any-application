import SwiftUI

struct TheaterFeedGridView: View {
    let dramas: [TheaterDrama]
    let isAppending: Bool
    let appendErrorMessage: String?
    let onTapDrama: (TheaterDrama) -> Void
    let onLoadMore: () -> Void

    private let columns = [
        GridItem(.flexible(minimum: 0, maximum: .infinity), spacing: 12, alignment: .top),
        GridItem(.flexible(minimum: 0, maximum: .infinity), spacing: 12, alignment: .top)
    ]

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 14) {
                ForEach(Array(dramas.enumerated()), id: \.element.id) { index, drama in
                    TheaterDramaCardView(drama: drama, index: index) {
                        onTapDrama(drama)
                    }
                    .frame(maxWidth: .infinity)
                    .onAppear {
                        if index == dramas.count - 1 {
                            onLoadMore()
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 16)
            .padding(.top, 8)

            footerView
                .padding(.horizontal, DesignTokens.Spacing.lg)
                .padding(.vertical, DesignTokens.Spacing.md)
        }
        .scrollIndicators(.hidden)
        .background(Color(red: 0.97, green: 0.97, blue: 0.97))
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
