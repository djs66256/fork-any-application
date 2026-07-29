import SwiftUI

struct MenuMessagePreviewView: View {
    let state: MenuPanelViewModel.MessagePreviewState
    let onTapMessages: () -> Void

    var body: some View {
        Button(action: onTapMessages) {
            HStack(alignment: .top, spacing: DesignTokens.Spacing.md) {
                Circle()
                    .fill(Color.red)
                    .frame(width: 10, height: 10)
                    .padding(.top, 6)

                VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                    Text(title)
                        .font(.headline)
                        .foregroundStyle(.primary)
                    Text(summary)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                    if let relativeTime {
                        Text(relativeTime)
                            .font(.footnote)
                            .foregroundStyle(.tertiary)
                    }
                }

                Spacer(minLength: DesignTokens.Spacing.sm)

                Image(systemName: "chevron.right")
                    .foregroundStyle(.tertiary)
                    .padding(.top, 4)
            }
            .padding(DesignTokens.Spacing.lg)
            .background(
                Color(.secondarySystemBackground),
                in: RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
            )
        }
        .buttonStyle(.plain)
    }

    private var title: String {
        switch state {
        case .content(let preview):
            return preview.title
        default:
            return "系统通知"
        }
    }

    private var summary: String {
        switch state {
        case .idle, .loading:
            return "正在加载最新消息…"
        case .content(let preview):
            return preview.summary
        case .empty:
            return "暂无消息"
        case .error(let fallback):
            return fallback
        }
    }

    private var relativeTime: String? {
        guard case .content(let preview) = state else { return nil }
        return preview.relativeTime
    }
}
