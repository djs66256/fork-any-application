import SwiftUI

/// Reusable drama card used by the home feed and search results.
struct HomeDramaCardView: View {
    let drama: Drama
    let onPlay: () -> Void
    let onDetail: () -> Void

    private var metadataText: String {
        var items: [String] = []

        if !drama.category.isEmpty {
            items.append(drama.category)
        }

        if let firstTag = drama.tags?.first, !firstTag.isEmpty {
            items.append("#\(firstTag)")
        }

        items.append("\(drama.episodeCount) 集")

        if let rating = drama.rating {
            items.append(String(format: "%.1f 分", rating))
        }

        return items.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.md) {
            DramaCoverView(coverURL: drama.coverUrl)

            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                Text(drama.title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(2)

                if !drama.description.isEmpty {
                    Text(drama.description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }

                if !metadataText.isEmpty {
                    Text(metadataText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            HStack(spacing: DesignTokens.Spacing.md) {
                Button("观看") {
                    onPlay()
                }
                .buttonStyle(.borderedProminent)

                Button("详情") {
                    onDetail()
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(DesignTokens.Spacing.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
    }
}

private struct DramaCoverView: View {
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
        .frame(maxWidth: .infinity)
        .frame(height: 180)
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
