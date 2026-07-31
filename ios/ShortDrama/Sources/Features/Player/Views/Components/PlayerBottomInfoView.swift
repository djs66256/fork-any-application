import SwiftUI

struct PlayerBottomInfoView: View {
    let title: String
    let hotComment: String
    let disclaimer: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(2)

            Text(hotComment)
                .font(.system(size: 14))
                .foregroundStyle(.white.opacity(0.88))
                .lineLimit(1)

            Text(disclaimer)
                .font(.system(size: 12))
                .foregroundStyle(.white.opacity(0.58))
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
