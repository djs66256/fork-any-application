import SwiftUI

struct PlayerBottomInfoView: View {
    let title: String
    let hotComment: String
    let disclaimer: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 6) {
                Text(title)
                    .font(.system(size: 21, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)

                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundStyle(.white.opacity(0.9))
            }

            if !hotComment.isEmpty {
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Image(systemName: "flame.fill")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(.white)

                    Text("热评：")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(.white)

                    Text(hotComment)
                        .font(.system(size: 15, weight: .regular))
                        .foregroundStyle(.white.opacity(0.92))
                        .lineLimit(1)

                    Text("展开")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(.white)
                }
                .lineLimit(1)
            }

            if !disclaimer.isEmpty {
                HStack(alignment: .center, spacing: 6) {
                    Image(systemName: "info.circle")
                        .font(.system(size: 15, weight: .regular))
                        .foregroundStyle(.white.opacity(0.6))
                    Text(disclaimer)
                        .font(.system(size: 14, weight: .regular))
                        .foregroundStyle(.white.opacity(0.6))
                        .lineLimit(1)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
