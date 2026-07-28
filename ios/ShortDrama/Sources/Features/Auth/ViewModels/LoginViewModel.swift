import Foundation

@MainActor
final class LoginViewModel: ObservableObject {
    enum ViewState: Equatable {
        case editing
        case sendingOtp
        case otpSent(cooldownRemaining: Int)
        case submitting
        case success
        case error(String)
    }

    private enum Constants {
        static let countryCode = "+86"
        static let scene = "login"
        static let phoneLength = 11
        static let codeLength = 6
    }

    @Published var phone = ""
    @Published var code = ""
    @Published var hasAcceptedAgreement = false
    @Published private(set) var viewState: ViewState = .editing
    @Published private(set) var phoneError: String?
    @Published private(set) var codeError: String?
    @Published private(set) var globalError: String?
    @Published private(set) var cooldownRemaining = 0

    private let sendOtpUseCase: SendOtpUseCase
    private let createSessionUseCase: CreateSessionUseCase
    private let onLoginSuccess: @MainActor @Sendable (AuthSession) async throws -> Void

    private var cooldownTask: Task<Void, Never>?

    init(
        sendOtpUseCase: SendOtpUseCase = SendOtpUseCase(repository: AuthRepository()),
        createSessionUseCase: CreateSessionUseCase = CreateSessionUseCase(repository: AuthRepository()),
        onLoginSuccess: @escaping @MainActor @Sendable (AuthSession) async throws -> Void
    ) {
        self.sendOtpUseCase = sendOtpUseCase
        self.createSessionUseCase = createSessionUseCase
        self.onLoginSuccess = onLoginSuccess
    }

    deinit {
        cooldownTask?.cancel()
    }

    var sendOtpButtonTitle: String {
        cooldownRemaining > 0 ? "重新发送（\(cooldownRemaining)s）" : "获取验证码"
    }

    var isOtpCoolingDown: Bool {
        cooldownRemaining > 0
    }

    var isSendingOtp: Bool {
        viewState == .sendingOtp
    }

    var isSubmitting: Bool {
        viewState == .submitting
    }

    var canSendOtp: Bool {
        isPhoneEligibleForSubmission
            && hasAcceptedAgreement
            && !isSendingOtp
            && !isSubmitting
            && !isOtpCoolingDown
    }

    var canSubmit: Bool {
        isPhoneEligibleForSubmission
            && isCodeEligibleForSubmission
            && hasAcceptedAgreement
            && !isSubmitting
    }

    func sendOtp() async {
        guard !isSendingOtp, !isSubmitting, !isOtpCoolingDown else { return }
        clearErrors()

        let isPhoneValid = validatePhone()
        let hasAgreement = validateAgreement()
        guard isPhoneValid, hasAgreement else {
            viewState = .editing
            return
        }

        viewState = .sendingOtp

        do {
            let result = try await sendOtpUseCase.execute(
                phone: normalizedPhone,
                countryCode: Constants.countryCode,
                scene: Constants.scene
            )
            startCooldown(seconds: result.cooldownSeconds)
        } catch let error as APIError {
            presentError(message: error.errorDescription ?? "验证码发送失败，请稍后重试")
        } catch {
            presentError(message: error.localizedDescription)
        }
    }

    func submit() async -> Bool {
        guard !isSubmitting else { return false }
        clearErrors()

        let isPhoneValid = validatePhone()
        let isCodeValid = validateCode()
        let hasAgreement = validateAgreement()
        guard isPhoneValid, isCodeValid, hasAgreement else {
            viewState = .editing
            return false
        }

        viewState = .submitting

        do {
            let session = try await createSessionUseCase.execute(
                phone: normalizedPhone,
                countryCode: Constants.countryCode,
                code: normalizedCode
            )
            try await onLoginSuccess(session)
            viewState = .success
            globalError = nil
            return true
        } catch let error as APIError {
            handleLoginError(error)
            return false
        } catch {
            presentError(message: error.localizedDescription)
            return false
        }
    }

    private var normalizedPhone: String {
        phone.filter(\.isNumber)
    }

    private var normalizedCode: String {
        code.filter(\.isNumber)
    }

    private var isPhoneEligibleForSubmission: Bool {
        let value = normalizedPhone
        return value.count == Constants.phoneLength && value.first == "1"
    }

    private var isCodeEligibleForSubmission: Bool {
        normalizedCode.count == Constants.codeLength
    }

    private func clearErrors() {
        phoneError = nil
        codeError = nil
        globalError = nil
        if !isOtpCoolingDown {
            viewState = .editing
        }
    }

    @discardableResult
    private func validatePhone() -> Bool {
        let value = normalizedPhone
        guard !value.isEmpty else {
            phoneError = "请输入手机号"
            return false
        }

        guard value.count == Constants.phoneLength else {
            phoneError = "请输入正确的 11 位手机号"
            return false
        }

        guard value.first == "1" else {
            phoneError = "请输入正确的手机号"
            return false
        }

        return true
    }

    @discardableResult
    private func validateCode() -> Bool {
        let value = normalizedCode
        guard !value.isEmpty else {
            codeError = "请输入验证码"
            return false
        }

        guard value.count == Constants.codeLength else {
            codeError = "请输入 6 位验证码"
            return false
        }

        return true
    }

    @discardableResult
    private func validateAgreement() -> Bool {
        guard hasAcceptedAgreement else {
            globalError = "请先阅读并同意用户协议与隐私政策"
            return false
        }

        return true
    }

    private func handleLoginError(_ error: APIError) {
        switch error.businessCode {
        case "AUTH_INVALID_CODE", "AUTH_CODE_EXPIRED":
            codeError = error.errorDescription ?? "验证码无效，请重新输入"
            viewState = .error(codeError ?? "验证码无效，请重新输入")
        default:
            presentError(message: error.errorDescription ?? "登录失败，请稍后重试")
        }
    }

    private func presentError(message: String) {
        globalError = message
        viewState = .error(message)
    }

    private func startCooldown(seconds: Int) {
        cooldownTask?.cancel()
        cooldownRemaining = max(seconds, 0)
        viewState = .otpSent(cooldownRemaining: cooldownRemaining)

        guard cooldownRemaining > 0 else {
            viewState = .editing
            return
        }

        cooldownTask = Task { [weak self] in
            guard let self else { return }

            while !Task.isCancelled, self.cooldownRemaining > 0 {
                try? await Task.sleep(for: .seconds(1))
                guard !Task.isCancelled else { return }

                await MainActor.run {
                    guard self.cooldownRemaining > 0 else { return }
                    self.cooldownRemaining -= 1
                    if self.cooldownRemaining > 0 {
                        if case .otpSent = self.viewState {
                            self.viewState = .otpSent(cooldownRemaining: self.cooldownRemaining)
                        }
                    } else if case .otpSent = self.viewState {
                        self.viewState = .editing
                    }
                }
            }
        }
    }
}
