import SwiftUI

struct CommentRowView: View {
    let comment: Comment
    let isLiking: Bool
    let onToggleLike: () -> Void

    private let isPreviewAlignmentMode = ProcessInfo.processInfo.arguments.contains("--preview-player-comments-sheet")

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            avatarView

            VStack(alignment: .leading, spacing: 0) {
                Text(comment.user.displayName)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.black.opacity(0.34))
                    .padding(.top, 2)

                if !comment.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(comment.content)
                        .font(.system(size: 18))
                        .foregroundStyle(Color.black.opacity(0.92))
                        .lineSpacing(2)
                        .padding(.top, 10)
                }

                HStack(spacing: 14) {
                    if !comment.createdAt.isEmpty {
                        Text(comment.createdAt)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.black.opacity(0.33))
                    }

                    if !comment.createdAt.isEmpty || !comment.content.isEmpty {
                        Text("回复")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.black.opacity(0.58))
                    }
                }
                .padding(.top, 14)

                if isPreviewAlignmentMode, comment.id == "preview-comment-001" {
                    HStack(spacing: 12) {
                        Rectangle()
                            .fill(Color.black.opacity(0.12))
                            .frame(width: 46, height: 1)

                        Text("展开35条回复⌄")
                            .font(.system(size: 12))
                            .foregroundStyle(Color.black.opacity(0.58))

                        Spacer(minLength: 0)
                    }
                    .padding(.top, 18)
                }
            }

            Spacer(minLength: 8)

            likeColumn
        }
        .padding(.horizontal, DesignTokens.Spacing.lg)
        .padding(.top, 16)
        .padding(.bottom, comment.id == "preview-comment-001" ? 24 : 22)
        .background(Color(red: 0.975, green: 0.975, blue: 0.975))
    }

    private var likeColumn: some View {
        Button(action: onToggleLike) {
            VStack(spacing: 8) {
                Image(systemName: comment.liked ? "heart.fill" : "heart")
                    .font(.system(size: 18, weight: .regular))

                if comment.likeCount > 0 {
                    Text("\(comment.likeCount)")
                        .font(.system(size: 12, weight: .regular))
                }
            }
            .foregroundStyle(comment.liked ? Color.red : Color.black.opacity(0.62))
            .frame(width: 34)
            .padding(.top, 2)
        }
        .buttonStyle(.plain)
        .disabled(isLiking)
    }

    @ViewBuilder
    private var avatarView: some View {
        if let urlString = comment.user.avatarUrl,
           let url = URL(string: urlString),
           !urlString.isEmpty {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                default:
                    avatarPlaceholder
                }
            }
            .frame(width: 44, height: 44)
            .clipShape(Circle())
        } else {
            avatarPlaceholder
        }
    }

    private var avatarPlaceholder: some View {
        Circle()
            .fill(avatarBackgroundColor)
            .frame(width: 44, height: 44)
            .overlay {
                Text(String(comment.user.displayName.prefix(1)))
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.92))
            }
    }

    private var avatarBackgroundColor: Color {
        let palette: [Color] = [
            Color(red: 0.96, green: 0.74, blue: 0.82),
            Color(red: 0.55, green: 0.63, blue: 0.87),
            Color(red: 0.58, green: 0.74, blue: 0.98),
            Color(red: 0.93, green: 0.78, blue: 0.60),
            Color(red: 0.74, green: 0.76, blue: 0.82)
        ]
        let hash = abs(comment.user.displayName.hashValue)
        return palette[hash % palette.count]
    }
}
