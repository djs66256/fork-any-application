import SwiftUI

struct RankingDramaCardView: View {
    let drama: RankingDrama
    let rankingType: RankingType
    let rankIndex: Int
    let onTapCard: () -> Void
    let onTapBooking: () -> Void

    private var categoryLine: String {
        let trimmed = drama.category.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "短剧" : trimmed
    }

    private var descriptionText: String {
        drama.description.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var bookingMetaText: String {
        let bookingText = "\(formattedCount(drama.playCount))人预约"
        let suffix = rankIndex <= 4 ? "预计\(10 + rankIndex)月上线" : "预计近期上线"
        return "预告 · \(bookingText) · \(suffix)"
    }

    var body: some View {
        Button(action: onTapCard) {
            HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
                rankingBadge
                    .padding(.top, 2)

                RankingCoverView(coverURL: drama.coverUrl)

                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .top, spacing: DesignTokens.Spacing.sm) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(drama.title)
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(Color.primary)
                                .lineLimit(2)
                                .multilineTextAlignment(.leading)

                            Text(categoryLine)
                                .font(.system(size: 14))
                                .foregroundStyle(Color.secondary)
                                .lineLimit(1)
                        }

                        Spacer(minLength: 0)

                        if rankingType == .booking {
                            HStack(spacing: 6) {
                                Image(systemName: "bell.fill")
                                    .font(.system(size: 12, weight: .bold))
                                Text("\(formattedCount(drama.bookingCount))期待")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .foregroundStyle(Color(red: 1.0, green: 0.49, blue: 0.18))
                            .fixedSize()
                        }
                    }

                    if !descriptionText.isEmpty {
                        Text(descriptionText)
                            .font(.system(size: 14))
                            .foregroundStyle(Color.secondary)
                            .lineLimit(2)
                            .multilineTextAlignment(.leading)
                    }

                    footerContent
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.vertical, 14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white)
        }
        .buttonStyle(.plain)
    }

    @ViewBuilder
    private var footerContent: some View {
        if rankingType == .booking {
            HStack(spacing: 10) {
                Text(bookingMetaText)
                    .font(.system(size: 14))
                    .foregroundStyle(Color(red: 0.76, green: 0.66, blue: 0.55))
                    .lineLimit(1)

                Spacer(minLength: 0)

                RankingBookingButton(
                    booked: drama.isBooked,
                    isSubmitting: drama.isBookingSubmitting,
                    action: onTapBooking
                )
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 8)
            .background {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Color(red: 0.98, green: 0.96, blue: 0.94))
            }
        } else {
            HStack(spacing: DesignTokens.Spacing.sm) {
                if let rating = drama.rating {
                    ratingChip(rating)
                }
                RankingMetricView(rankingType: rankingType, drama: drama)
            }
        }
    }

    private var rankingBadge: some View {
        ZStack(alignment: .topLeading) {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(rankBadgeBackground)
                .frame(width: 32, height: 32)

            Text("\(rankIndex)")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(Color.white)
                .frame(width: 32, height: 32)
        }
    }

    private var rankBadgeBackground: Color {
        switch rankIndex {
        case 1:
            return Color(red: 1.0, green: 0.68, blue: 0.23)
        case 2:
            return Color(red: 0.16, green: 0.80, blue: 0.69)
        case 3:
            return Color(red: 0.30, green: 0.61, blue: 1.0)
        default:
            return Color(red: 0.21, green: 0.21, blue: 0.23)
        }
    }

    private func ratingChip(_ rating: Double) -> some View {
        HStack(spacing: 4) {
            Image(systemName: "star.fill")
                .font(.system(size: 11, weight: .bold))
            Text("评分\(String(format: "%.1f", rating))")
                .font(.system(size: 14, weight: .medium))
        }
        .foregroundStyle(Color(red: 1.0, green: 0.49, blue: 0.18))
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color(red: 1.0, green: 0.96, blue: 0.90))
        }
    }

    private func formattedCount(_ value: Int) -> String {
        if value >= 10_000 {
            return String(format: "%.1f万", Double(value) / 10_000)
                .replacingOccurrences(of: ".0", with: "")
        }
        return "\(value)"
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
        .frame(width: 104, height: 138)
        .clipped()
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    private var placeholder: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(Color(.tertiarySystemFill))
            Image(systemName: "photo")
                .font(.system(size: 40))
                .foregroundStyle(.secondary)
        }
    }
}
