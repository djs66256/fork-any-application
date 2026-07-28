import SwiftUI

struct TheaterChannelTabBar: View {
    let channels: [TheaterChannel]
    let selectedChannel: TheaterChannel
    let onSelect: (TheaterChannel) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignTokens.Spacing.sm) {
                ForEach(channels) { channel in
                    Button {
                        onSelect(channel)
                    } label: {
                        Text(channel.title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(channel == selectedChannel ? .white : .primary)
                            .padding(.horizontal, DesignTokens.Spacing.md)
                            .padding(.vertical, DesignTokens.Spacing.sm)
                            .background(background(for: channel))
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
        }
    }

    private func background(for channel: TheaterChannel) -> some ShapeStyle {
        channel == selectedChannel ? Color.accentColor : Color(.secondarySystemBackground)
    }
}

#Preview {
    TheaterChannelTabBar(
        channels: TheaterChannel.allCases,
        selectedChannel: .all,
        onSelect: { _ in }
    )
}
