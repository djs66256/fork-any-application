import SwiftUI

struct PlayerEpisodeDock: View {
    let title: String
    let seriesStatus: String
    let totalCount: Int
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                Text(title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(.white)

                Text("\(seriesStatus) 全\(totalCount)集")
                    .font(.system(size: 14))
                    .foregroundStyle(.white.opacity(0.68))

                Spacer()

                Image(systemName: "chevron.up")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .padding(.horizontal, 18)
            .frame(height: 54)
            .background(Color.black.opacity(0.42))
            .clipShape(RoundedRectangle(cornerRadius: 27, style: .continuous))
        }
        .buttonStyle(.plain)
    }
}
