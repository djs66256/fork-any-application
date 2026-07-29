import SwiftUI

struct CommentRowView: View {
    let comment: Comment
    let isLiking: Bool
    let onToggleLike: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
            Circle()
                .fill(Color(.tertiarySystemFill))
                .frame(width: 36, height: 36)
                .overlay {
                    Text(String(comment.user.displayName.prefix(1)))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

            VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                Text(comment.user.displayName)
                    .font(.subheadline.weight(.medium))

                Text(comment.content)
                    .font(.body)
                    .foregroundStyle(.primary)

                Text(comment.createdAt)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: DesignTokens.Spacing.sm)

            Button(action: onToggleLike) {
                VStack(spacing: DesignTokens.Spacing.xs) {
                    Image(systemName: comment.liked ? "heart.fill" : "heart")
                    Text("\(comment.likeCount)")
                        .font(.caption)
                }
                .foregroundStyle(comment.liked ? Color.red : Color.secondary)
            }
            .buttonStyle(.plain)
            .disabled(isLiking)
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.vertical, DesignTokens.Spacing.md)
    }
}
