import SwiftUI

struct RecentlyViewedCardView: View {
    let item: RecentlyViewedItem
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
                coverView

                VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                    Text(item.title)
                        .font(.headline)
                        .foregroundStyle(.primary)
                        .multilineTextAlignment(.leading)
                        .lineLimit(2)

                    Text(item.episodeProgressText)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)

                    if item.progress > 0 {
                        Text("续播进度 \(Int(item.progress)) 秒")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                }

                Spacer(minLength: DesignTokens.Spacing.sm)
            }
            .padding(DesignTokens.Spacing.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                Color(.secondarySystemBackground),
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
            )
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var coverView: some View {
        if let coverURL = item.coverURL,
           let url = URL(string: coverURL) {
            AsyncImage(url: url) { image in
                image
                    .resizable()
                    .scaledToFill()
            } placeholder: {
                placeholderCover
            }
            .frame(width: 84, height: 112)
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))
        } else {
            placeholderCover
                .frame(width: 84, height: 112)
        }
    }

    private var placeholderCover: some View {
        RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md)
            .fill(Color.accentColor.opacity(0.12))
            .overlay {
                Image(systemName: "play.rectangle")
                    .font(.system(size: 24))
                    .foregroundStyle(Color.accentColor)
            }
    }
}
