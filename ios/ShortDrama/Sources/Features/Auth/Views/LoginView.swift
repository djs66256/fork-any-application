import SwiftUI

struct LoginView: View {
    let context: LoginInterceptionContext?
    let onClose: () -> Void
    let onSuccess: () -> Void

    @StateObject private var viewModel: LoginViewModel

    init(
        context: LoginInterceptionContext? = nil,
        onClose: @escaping () -> Void,
        onSuccess: @escaping () -> Void,
        onLoginSuccess: @escaping @MainActor @Sendable (AuthSession) async throws -> Void,
        sendOtpUseCase: SendOtpUseCase = SendOtpUseCase(repository: AuthRepository()),
        createSessionUseCase: CreateSessionUseCase = CreateSessionUseCase(repository: AuthRepository())
    ) {
        self.context = context
        self.onClose = onClose
        self.onSuccess = onSuccess
        _viewModel = StateObject(
            wrappedValue: LoginViewModel(
                sendOtpUseCase: sendOtpUseCase,
                createSessionUseCase: createSessionUseCase,
                onLoginSuccess: onLoginSuccess
            )
        )
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignTokens.Spacing.lg) {
                headerSection
                copySection
                phoneInputSection
                codeInputSection
                agreementSection
                submitButton
                errorSection
            }
            .padding(DesignTokens.Spacing.lg)
        }
        .background(Color(.systemBackground))
    }

    private var headerSection: some View {
        HStack {
            Button("关闭") {
                onClose()
            }
            .buttonStyle(.plain)

            Spacer()

            Text("手机号登录")
                .font(.headline)

            Spacer()

            Color.clear
                .frame(width: 32, height: 24)
        }
    }

    private var copySection: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text("登录后可同步你的播放记录与预约状态")
                .font(.title3)
                .fontWeight(.semibold)

            Text(subtitleText)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
    }

    private var phoneInputSection: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text("手机号")
                .font(.subheadline)
                .fontWeight(.medium)

            HStack(spacing: DesignTokens.Spacing.md) {
                Text("+86")
                    .foregroundStyle(.secondary)

                TextField("请输入手机号", text: $viewModel.phone)
                    .keyboardType(.numberPad)
                    .textContentType(.telephoneNumber)
            }
            .padding(DesignTokens.Spacing.md)
            .background(Color(.secondarySystemBackground))
            .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))

            if let phoneError = viewModel.phoneError {
                Text(phoneError)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
    }

    private var codeInputSection: some View {
        VStack(alignment: .leading, spacing: DesignTokens.Spacing.sm) {
            Text("验证码")
                .font(.subheadline)
                .fontWeight(.medium)

            HStack(spacing: DesignTokens.Spacing.sm) {
                TextField("请输入验证码", text: $viewModel.code)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .padding(DesignTokens.Spacing.md)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: DesignTokens.CornerRadius.md))

                Button(viewModel.isSendingOtp ? "发送中…" : viewModel.sendOtpButtonTitle) {
                    Task {
                        await viewModel.sendOtp()
                    }
                }
                .buttonStyle(.bordered)
                .disabled(!viewModel.canSendOtp)
            }

            if let codeError = viewModel.codeError {
                Text(codeError)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
    }

    private var agreementSection: some View {
        Toggle(isOn: $viewModel.hasAcceptedAgreement) {
            Text("我已阅读并同意《用户协议》和《隐私政策》")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .toggleStyle(.checkbox)
    }

    private var submitButton: some View {
        Button(viewModel.isSubmitting ? "登录中…" : "立即登录") {
            Task {
                if await viewModel.submit() {
                    onSuccess()
                }
            }
        }
        .buttonStyle(.borderedProminent)
        .disabled(!viewModel.canSubmit)
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private var errorSection: some View {
        if let globalError = viewModel.globalError {
            Text(globalError)
                .font(.footnote)
                .foregroundStyle(.red)
                .multilineTextAlignment(.leading)
        }
    }

    private var subtitleText: String {
        switch context?.source {
        case .rankingBooking:
            return "登录后即可继续完成预约。"
        case .profileEntry:
            return "登录后可在“我的”中查看你的账号信息。"
        case .unknown, .none:
            return "验证码仅用于本次登录验证。"
        }
    }
}

private struct CheckboxToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            HStack(alignment: .top, spacing: DesignTokens.Spacing.sm) {
                Image(systemName: configuration.isOn ? "checkmark.square.fill" : "square")
                    .foregroundStyle(configuration.isOn ? Color.accentColor : Color.secondary)
                configuration.label
            }
        }
        .buttonStyle(.plain)
    }
}

private extension ToggleStyle where Self == CheckboxToggleStyle {
    static var checkbox: CheckboxToggleStyle { CheckboxToggleStyle() }
}

#Preview {
    LoginView(
        context: LoginInterceptionContext(source: .profileEntry),
        onClose: {},
        onSuccess: {},
        onLoginSuccess: { _ in }
    )
}
