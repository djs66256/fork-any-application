import SwiftUI

struct CheckInPopupView: View {
    let state: HomeViewModel.CheckInPopupState
    let onClose: () -> Void
    let onSubmit: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.35)
                .ignoresSafeArea()

            VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
                HStack {
                    VStack(alignment: .leading, spacing: DesignTokens.Spacing.xs) {
                        Text("签到奖励")
                            .font(.title3)
                            .fontWeight(.semibold)
                        Text(state.rewardCopy)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button(action: onClose) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                    .buttonStyle(.plain)
                }

                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: DesignTokens.Spacing.sm), count: 4), spacing: DesignTokens.Spacing.sm) {
                    ForEach(state.days, id: \.day) { day in
                        VStack(spacing: DesignTokens.Spacing.xs) {
                            Text(day.title)
                                .font(.caption)
                                .fontWeight(.medium)
                            Text(day.rewardLabel)
                                .font(.caption2)
                                .multilineTextAlignment(.center)
                                .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity, minHeight: 64)
                        .padding(DesignTokens.Spacing.sm)
                        .background(backgroundColor(for: day.status))
                        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))
                    }
                }

                if let message = state.feedbackMessage {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(state.isError ? .red : .green)
                }

                Button(state.buttonTitle) {
                    onSubmit()
                }
                .buttonStyle(.borderedProminent)
                .disabled(state.isSubmitDisabled)
                .frame(maxWidth: .infinity)
            }
            .padding(DesignTokens.Spacing.xl)
            .frame(maxWidth: 360)
            .background(Color(.systemBackground))
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
            .padding(DesignTokens.Spacing.lg)
        }
    }

    private func backgroundColor(for status: SignInDay.Status) -> Color {
        switch status {
        case .signed:
            return Color.green.opacity(0.18)
        case .today:
            return Color.accentColor.opacity(0.18)
        case .locked:
            return Color(.secondarySystemBackground)
        }
    }
}
