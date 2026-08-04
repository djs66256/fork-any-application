import SwiftUI

struct CheckInPopupView: View {
    let state: HomeViewModel.CheckInPopupState
    let onClose: () -> Void
    let onSubmit: () -> Void

    private let popupGradient = LinearGradient(
        colors: [
            Color(red: 1.0, green: 0.56, blue: 0.23),
            Color(red: 1.0, green: 0.74, blue: 0.41)
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    private let buttonGradient = LinearGradient(
        colors: [
            Color(red: 1.0, green: 0.96, blue: 0.83),
            Color(red: 0.96, green: 0.91, blue: 0.73)
        ],
        startPoint: .top,
        endPoint: .bottom
    )

    var body: some View {
        GeometryReader { proxy in
            let topPadding = max(proxy.safeAreaInsets.top, DesignTokens.Spacing.lg)
            let bottomPadding = max(proxy.safeAreaInsets.bottom, DesignTokens.Spacing.xl)
            let availableHeight = max(proxy.size.height - topPadding - bottomPadding, 0)

            ZStack {
                Color.black.opacity(0.6)
                    .ignoresSafeArea()

                ScrollView(.vertical, showsIndicators: false) {
                    VStack(spacing: DesignTokens.Spacing.lg) {
                        VStack(spacing: DesignTokens.Spacing.lg) {
                            titleSection
                            rewardGrid
                            claimButton
                            footerText
                        }
                        .padding(.horizontal, DesignTokens.Spacing.xl)
                        .padding(.top, DesignTokens.Spacing.xxl)
                        .padding(.bottom, DesignTokens.Spacing.xl)
                        .frame(maxWidth: 360)
                        .background(popupBackground)
                        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xxl))
                        .overlay {
                            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xxl)
                                .stroke(Color.white.opacity(0.28), lineWidth: 1)
                        }
                        .shadow(color: .black.opacity(0.24), radius: 20, y: 12)

                        Button(action: onClose) {
                            ZStack {
                                Circle()
                                    .fill(Color.black.opacity(0.2))
                                    .frame(width: 56, height: 56)
                                Circle()
                                    .stroke(Color.white.opacity(0.4), lineWidth: 1)
                                    .frame(width: 56, height: 56)
                                Image(systemName: "xmark")
                                    .font(.system(size: 22, weight: .medium))
                                    .foregroundStyle(.white)
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("关闭签到弹窗")
                    }
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: availableHeight, alignment: .center)
                    .padding(.horizontal, DesignTokens.Spacing.xl)
                    .padding(.top, topPadding)
                    .padding(.bottom, bottomPadding)
                }
                .scrollBounceBehavior(.basedOnSize)
            }
        }
    }

    private var titleSection: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            Text("7天签到必得6万金币")
                .font(.system(size: 24, weight: .bold))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)

            Text(state.rewardCopy)
                .font(.subheadline)
                .foregroundStyle(Color.white.opacity(0.88))
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }

    private var rewardGrid: some View {
        let columns = Array(repeating: GridItem(.flexible(), spacing: DesignTokens.Spacing.sm), count: 4)

        return LazyVGrid(columns: columns, spacing: DesignTokens.Spacing.sm) {
            ForEach(state.days, id: \.day) { day in
                signInCell(for: day)
            }
        }
        .padding(DesignTokens.Spacing.md)
        .background(Color.white.opacity(0.82))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xl))
    }

    private var claimButton: some View {
        Button(action: onSubmit) {
            Text(state.buttonTitle)
                .font(.system(size: 20, weight: .bold))
                .foregroundStyle(Color(red: 0.47, green: 0.24, blue: 0.04))
                .frame(maxWidth: .infinity)
                .frame(height: 72)
                .background(buttonGradient)
                .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.xl))
        }
        .buttonStyle(.plain)
        .disabled(state.isSubmitDisabled)
        .opacity(state.isSubmitDisabled ? 0.7 : 1)
    }

    private var footerText: some View {
        VStack(spacing: DesignTokens.Spacing.sm) {
            if let message = state.feedbackMessage {
                Text(message)
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(state.isError ? Color(red: 0.64, green: 0.14, blue: 0.04) : Color.white)
            }

            Text("金币奖励可在「福利」查看")
                .font(.footnote)
                .foregroundStyle(Color.white.opacity(0.82))
        }
    }

    private var popupBackground: some View {
        ZStack {
            popupGradient

            Circle()
                .fill(Color.white.opacity(0.12))
                .frame(width: 240, height: 240)
                .offset(x: 88, y: -72)

            Circle()
                .fill(Color.white.opacity(0.08))
                .frame(width: 300, height: 300)
                .offset(x: -150, y: 140)
        }
    }

    private func signInCell(for day: SignInDay) -> some View {
        let isToday = day.status == .today
        let isSigned = day.status == .signed
        let isLocked = day.status == .locked

        return VStack(spacing: DesignTokens.Spacing.sm) {
            Text(amountText(for: day))
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(amountColor(isToday: isToday, isLocked: isLocked))

            ZStack {
                Circle()
                    .fill(iconBackground(isToday: isToday, isSigned: isSigned, isLocked: isLocked))
                    .frame(width: 34, height: 34)
                Image(systemName: iconName(for: day))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(iconForeground(isToday: isToday, isLocked: isLocked))
            }

            Text(dayTitle(for: day, isToday: isToday))
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(titleColor(isToday: isToday, isLocked: isLocked))
        }
        .frame(maxWidth: .infinity, minHeight: 112)
        .padding(.vertical, DesignTokens.Spacing.md)
        .background(cellBackground(isToday: isToday, isSigned: isSigned, isLocked: isLocked))
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg))
        .overlay {
            RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.lg)
                .stroke(borderColor(isToday: isToday, isSigned: isSigned), lineWidth: isToday ? 2 : 1)
        }
    }

    private func amountText(for day: SignInDay) -> String {
        day.rewardLabel.replacingOccurrences(of: "金币", with: "")
    }

    private func dayTitle(for day: SignInDay, isToday: Bool) -> String {
        isToday ? "今日可领" : day.title
    }

    private func iconName(for day: SignInDay) -> String {
        if day.day == 2 || day.day == 7 {
            return "trophy.fill"
        }
        return "sparkles"
    }

    private func cellBackground(isToday: Bool, isSigned: Bool, isLocked: Bool) -> Color {
        if isToday {
            return Color(red: 1.0, green: 0.49, blue: 0.2)
        }
        if isSigned {
            return Color(red: 0.96, green: 0.72, blue: 0.56)
        }
        if isLocked {
            return Color.white.opacity(0.95)
        }
        return Color.white
    }

    private func borderColor(isToday: Bool, isSigned: Bool) -> Color {
        if isToday {
            return Color(red: 1.0, green: 0.83, blue: 0.47)
        }
        if isSigned {
            return Color.white.opacity(0.28)
        }
        return Color.black.opacity(0.05)
    }

    private func amountColor(isToday: Bool, isLocked: Bool) -> Color {
        if isToday {
            return .white
        }
        return isLocked ? Color(red: 0.95, green: 0.58, blue: 0.04) : Color(red: 0.98, green: 0.55, blue: 0.06)
    }

    private func titleColor(isToday: Bool, isLocked: Bool) -> Color {
        if isToday {
            return .white
        }
        return isLocked ? Color(red: 0.21, green: 0.16, blue: 0.12) : Color.white.opacity(0.95)
    }

    private func iconBackground(isToday: Bool, isSigned: Bool, isLocked: Bool) -> Color {
        if isToday {
            return Color.white.opacity(0.2)
        }
        if isSigned {
            return Color.white.opacity(0.18)
        }
        if isLocked {
            return Color(red: 1.0, green: 0.69, blue: 0.28).opacity(0.18)
        }
        return Color(red: 1.0, green: 0.69, blue: 0.28).opacity(0.18)
    }

    private func iconForeground(isToday: Bool, isLocked: Bool) -> Color {
        if isToday {
            return .white
        }
        return isLocked ? Color(red: 1.0, green: 0.62, blue: 0.09) : Color(red: 1.0, green: 0.95, blue: 0.74)
    }
}
