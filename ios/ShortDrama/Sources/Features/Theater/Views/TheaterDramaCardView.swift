import SwiftUI

struct TheaterDramaCardView: View {
    let drama: TheaterDrama
    let onTap: () -> Void

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
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
                coverView

                Text(TheaterHeatFormatter.string(from: drama.heat))
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.orange)

                Text(drama.title)
                    .font(.headline)
                    .foregroundStyle(.primary)
                    .lineLimit(2)

                if !metadataText.isEmpty {
                    Text(metadataText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DesignTokens.Spacing.md)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
        }
        .buttonStyle(.plain)
    }

    private var coverView: some View {
        Group {
            if let coverUrl = drama.coverUrl, let url = URL(string: coverUrl), !coverUrl.isEmpty {
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

#Preview {
    TheaterDramaCardView(
        drama: TheaterDrama(
            id: "preview",
            title: "逆袭归来后我成了豪门团宠",
            description: "描述",
            coverUrl: nil,
            category: "都市",
            episodeCount: 68,
            tags: ["逆袭", "豪门"],
            rating: 8.9,
            createdAt: "2026-07-25T00:00:00Z",
            updatedAt: "2026-07-25T00:00:00Z",
            heat: 98_210
        ),
        onTap: {}
    )
}
