import SwiftUI

struct PlayerRightActionBar: View {
    let liked: Bool
    let favorited: Bool
    let favoriteCountText: String
    let commentCountText: String
    let likeCountText: String
    let shareCountText: String
    let onLike: () -> Void
    let onFavorite: () -> Void
    let onComment: () -> Void

    var body: some View {
        VStack(spacing: 28) {
            actionButton(
                systemName: favorited ? "star.fill" : "star.fill",
                title: favoriteCountText,
                action: onFavorite
            )
            actionButton(
                systemName: "ellipsis.message.fill",
                title: commentCountText,
                action: onComment
            )
            actionButton(
                systemName: liked ? "heart.fill" : "heart.fill",
                title: likeCountText,
                action: onLike
            )
            staticButton(systemName: "arrowshape.turn.up.right.fill", title: shareCountText)
        }
        .padding(.bottom, 2)
    }

    private func actionButton(systemName: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            label(systemName: systemName, title: title)
        }
        .buttonStyle(.plain)
    }

    private func staticButton(systemName: String, title: String) -> some View {
        label(systemName: systemName, title: title)
    }

    private func label(systemName: String, title: String) -> some View {
        VStack(spacing: 10) {
            Image(systemName: systemName)
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 44, height: 44)

            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.white)
        }
        .frame(width: 62)
    }
}
