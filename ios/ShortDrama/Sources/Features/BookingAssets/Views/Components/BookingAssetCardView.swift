import SwiftUI

struct BookingAssetCardView: View {
    let asset: BookingAsset

    private var metadataText: String {
        ["\(asset.episodeCount) 集", formattedBookedAt]
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }

    var body: some View {
        HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
            coverView

            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                Text(asset.title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(2)

                Text(asset.availabilityStatus.title)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(asset.availabilityStatus == .online ? Color.green : Color.orange)
                    .padding(.horizontal, DesignTokens.Spacing.sm)
                    .padding(.vertical, DesignTokens.Spacing.xs)
                    .background(
                        (asset.availabilityStatus == .online ? Color.green : Color.orange)
                            .opacity(0.12)
                    )
                    .clipShape(Capsule())

                Text(metadataText)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }

            Spacer(minLength: 0)
        }
        .padding(DesignTokens.Spacing.lg)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
    }

    private var coverView: some View {
        Group {
            if let coverURL = asset.coverURL,
               let url = URL(string: coverURL),
               !coverURL.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        coverPlaceholder
                    }
                }
            } else {
                coverPlaceholder
            }
        }
        .frame(width: 92, height: 124)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))
    }

    private var coverPlaceholder: some View {
        ZStack {
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md)
                .fill(Color(.tertiarySystemFill))
            Image(systemName: "photo")
                .font(.system(size: DesignTokens.IconSize.md))
                .foregroundStyle(.secondary)
        }
    }

    private var formattedBookedAt: String {
        guard let bookedDate = Self.bookedDate(from: asset.bookedAt) else {
            return "预约时间未知"
        }

        return Self.displayFormatter.localizedString(for: bookedDate, relativeTo: Date())
    }
}

private extension BookingAssetCardView {
    static func bookedDate(from value: String) -> Date? {
        if let date = fractionalISO8601Formatter.date(from: value) {
            return date
        }

        return iso8601Formatter.date(from: value)
    }

    static let fractionalISO8601Formatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    static let iso8601Formatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }()

    static let displayFormatter: RelativeDateTimeFormatter = {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter
    }()
}
