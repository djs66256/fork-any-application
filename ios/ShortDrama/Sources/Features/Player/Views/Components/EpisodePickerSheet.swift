import SwiftUI

struct EpisodePickerSheet: View {
    let episodes: [Episode]
    let currentEpisodeId: String?
    let onSelect: (Episode) -> Void

    var body: some View {
        NavigationStack {
            List(episodes) { episode in
                Button {
                    onSelect(episode)
                } label: {
                    HStack(spacing: DesignTokens.Spacing.md) {
                        VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                            Text("第 \(episode.episodeNumber) 集")
                                .font(.headline)
                            Text(episode.title)
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                        }

                        Spacer()

                        if !episode.isPlayable {
                            Text("暂无资源")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        } else if currentEpisodeId == episode.id {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.blue)
                        }
                    }
                    .opacity(episode.isPlayable ? 1 : 0.5)
                }
                .disabled(!episode.isPlayable)
            }
            .navigationTitle("选集")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
