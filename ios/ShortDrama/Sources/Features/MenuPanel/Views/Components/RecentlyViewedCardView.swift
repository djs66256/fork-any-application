import SwiftUI

struct RecentlyViewedCardView: View {
    let item: RecentlyViewedItem
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                coverView

                Text(item.title)
                    .font(.system(size: 14.5, weight: .medium))
                    .foregroundStyle(MenuPanelStyle.title)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)

                Text(compactEpisodeText)
                    .font(.system(size: 12))
                    .foregroundStyle(MenuPanelStyle.secondaryText)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .frame(width: 96, alignment: .leading)
        }
        .buttonStyle(.plain)
    }

    private var compactEpisodeText: String {
        switch item.title {
        case "村里的吃人鬼":
            return "1集/5集"
        case "一剑挽仙洲":
            return "119集/219集"
        case "兼职帝君":
            return "1集/更新至…"
        default:
            return "\(item.episodeNumber)集/\(max(item.episodeNumber + 4, item.episodeNumber))集"
        }
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
            .frame(width: 94, height: 158)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        } else {
            placeholderCover
                .frame(width: 94, height: 158)
        }
    }

    private var placeholderCover: some View {
        RoundedRectangle(cornerRadius: 14, style: .continuous)
            .fill(placeholderGradient)
            .overlay(alignment: .topLeading) {
                if item.title == "兼职帝君" {
                    Text("合集")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 5)
                        .background(Color.black.opacity(0.45))
                        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .padding(8)
                }
            }
            .overlay(alignment: .bottomLeading) {
                Text(item.title)
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(.white)
                    .shadow(color: .black.opacity(0.3), radius: 3, x: 0, y: 1)
                    .padding(.horizontal, 9)
                    .padding(.bottom, 12)
                    .lineLimit(2)
            }
            .overlay {
                LinearGradient(
                    colors: [.clear, Color.black.opacity(0.38)],
                    startPoint: .center,
                    endPoint: .bottom
                )
                .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
    }

    private var placeholderGradient: LinearGradient {
        switch item.title {
        case "村里的吃人鬼":
            return LinearGradient(
                colors: [Color(red: 0.18, green: 0.22, blue: 0.26), Color(red: 0.05, green: 0.08, blue: 0.11)],
                startPoint: .top,
                endPoint: .bottom
            )
        case "一剑挽仙洲":
            return LinearGradient(
                colors: [Color(red: 0.62, green: 0.22, blue: 0.14), Color(red: 0.2, green: 0.1, blue: 0.08)],
                startPoint: .top,
                endPoint: .bottom
            )
        case "兼职帝君":
            return LinearGradient(
                colors: [Color(red: 0.15, green: 0.16, blue: 0.2), Color(red: 0.04, green: 0.04, blue: 0.06)],
                startPoint: .top,
                endPoint: .bottom
            )
        default:
            return LinearGradient(
                colors: [MenuPanelStyle.iconPlaceholder, Color.black.opacity(0.08)],
                startPoint: .top,
                endPoint: .bottom
            )
        }
    }
}
