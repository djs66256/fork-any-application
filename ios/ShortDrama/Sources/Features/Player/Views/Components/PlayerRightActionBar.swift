import SwiftUI

struct PlayerRightActionBar: View {
    let liked: Bool
    let favorited: Bool
    let onLike: () -> Void
    let onFavorite: () -> Void
    let onComment: () -> Void

    var body: some View {
        VStack(spacing: DesignTokens.Spacing.lg) {
            actionButton(systemName: liked ? "heart.fill" : "heart", title: "点赞", action: onLike)
            actionButton(systemName: favorited ? "star.fill" : "star", title: "收藏", action: onFavorite)
            actionButton(systemName: "message", title: "评论", action: onComment)
            staticButton(systemName: "square.and.arrow.up", title: "分享")
        }
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
        VStack(spacing: DesignTokens.Spacing.xs) {
            Image(systemName: systemName)
                .font(.title3)
            Text(title)
                .font(.caption)
        }
        .foregroundStyle(.white)
        .padding(.vertical, DesignTokens.Spacing.sm)
        .frame(width: 56)
        .background(Color.black.opacity(0.35))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
    }
}
