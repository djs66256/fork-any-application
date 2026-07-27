import SwiftUI

struct RankingDramaCardView: View {
    let drama: RankingDrama
    let rankingType: RankingType
    let rankIndex: Int
    let onTapCard: () -> Void
    let onTapBooking: () -> Void

    private var metadataText: String {
        var items: [String] = []

        if !drama.category.isEmpty {
            items.append(drama.category)
        }

        if let firstTag = drama.tags?.first, !firstTag.isEmpty {
            items.append("#\(firstTag)")
        }

        items.append("\(drama.episodeCount) 集")

        return items.joined(separator: " · ")
    }

    var body: some View {
        HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
            Text("#\(rankIndex)")
                .font(.headline.weight(.bold))
                .foregroundStyle(rankIndex <= 3 ? .orange : .secondary)
                .frame(width: 36, alignment: .leading)

            RankingCoverView(coverURL: drama.coverUrl)

            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                Text(drama.title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(2)

                if !drama.description.isEmpty {
                    Text(drama.description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }

                if !metadataText.isEmpty {
                    Text(metadataText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                RankingMetricView(rankingType: rankingType, drama: drama)

                if rankingType == .booking {
                    RankingBookingButton(
                        booked: drama.isBooked,
                        isSubmitting: drama.isBookingSubmitting,
                        action: onTapBooking
                    )
                }
            }

            Spacer(minLength: 0)
        }
        .padding(DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
        .contentShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
        .onTapGesture(perform: onTapCard)
    }
}

private struct RankingCoverView: View {
    let coverURL: String

    var body: some View {
        Group {
            if let url = URL(string: coverURL), !coverURL.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: 88, height: 120)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))
    }

    private var placeholder: some View {
        ZStack {
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md)
                .fill(Color(.tertiarySystemFill))
            Image(systemName: "photo")
                .font(.system(size: DesignTokens.IconSize.lg))
                .foregroundStyle(.secondary)
        }
    }
}
