import SwiftUI

struct TheaterChannelTabBar: View {
    let channels: [TheaterChannel]
    let selectedChannel: TheaterChannel
    let onSelect: (TheaterChannel) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 21) {
                ForEach(channels) { channel in
                    Button {
                        onSelect(channel)
                    } label: {
                        channelLabel(for: channel)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, DesignTokens.Spacing.lg)
            .padding(.trailing, 72)
            .padding(.top, 1)
            .padding(.bottom, 1)
        }
        .scrollIndicators(.hidden)
    }

    @ViewBuilder
    private func channelLabel(for channel: TheaterChannel) -> some View {
        let isSelected = channel == selectedChannel

        ZStack(alignment: .leading) {
            if isSelected {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [
                                Color(red: 1.0, green: 0.78, blue: 0.72).opacity(0.95),
                                Color(red: 0.85, green: 0.95, blue: 0.93).opacity(0.95)
                            ],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: 28, height: 28)
                    .offset(x: -5, y: -1)
            }

            Text(channel.title)
                .font(.system(size: isSelected ? 18 : 16, weight: isSelected ? .bold : .semibold))
                .tracking(isSelected ? -0.3 : 0)
                .foregroundStyle(isSelected ? Color.black.opacity(0.96) : Color.black.opacity(0.38))
                .frame(height: 30)
        }
        .fixedSize(horizontal: true, vertical: false)
    }
}

#Preview {
    TheaterChannelTabBar(
        channels: TheaterChannel.allCases,
        selectedChannel: .all,
        onSelect: { _ in }
    )
}
