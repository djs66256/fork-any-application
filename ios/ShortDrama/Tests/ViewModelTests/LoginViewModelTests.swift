import Foundation
@testable import ShortDrama
import Testing

@MainActor
struct LoginViewModelTests {
    @Test("login view model validates phone before sending otp")
    func testSendOtpRequiresValidPhone() async {
        let repository = MockAuthRepository()
        let viewModel = LoginViewModel(
            sendOtpUseCase: SendOtpUseCase(repository: repository),
            createSessionUseCase: CreateSessionUseCase(repository: repository),
            onLoginSuccess: { _ in }
        )

        viewModel.phone = "123"
        viewModel.hasAcceptedAgreement = true
        await viewModel.sendOtp()

        #expect(viewModel.phoneError == "请输入正确的 11 位手机号")
        #expect(repository.sendOtpCallCount == 0)
    }

    @Test("login view model blocks sending otp when agreement is unchecked")
    func testSendOtpRequiresAgreement() async {
        let repository = MockAuthRepository()
        let viewModel = LoginViewModel(
            sendOtpUseCase: SendOtpUseCase(repository: repository),
            createSessionUseCase: CreateSessionUseCase(repository: repository),
            onLoginSuccess: { _ in }
        )

        viewModel.phone = "13800138000"
        await viewModel.sendOtp()

        #expect(viewModel.globalError == "请先阅读并同意用户协议与隐私政策")
        #expect(repository.sendOtpCallCount == 0)
    }

    @Test("login view model exposes button availability from form state")
    func testButtonAvailabilityTracksFormState() {
        let repository = MockAuthRepository()
        let viewModel = LoginViewModel(
            sendOtpUseCase: SendOtpUseCase(repository: repository),
            createSessionUseCase: CreateSessionUseCase(repository: repository),
            onLoginSuccess: { _ in }
        )

        #expect(viewModel.canSendOtp == false)
        #expect(viewModel.canSubmit == false)

        viewModel.phone = "13800138000"
        #expect(viewModel.canSendOtp == false)
        #expect(viewModel.canSubmit == false)

        viewModel.hasAcceptedAgreement = true
        #expect(viewModel.canSendOtp == true)
        #expect(viewModel.canSubmit == false)

        viewModel.code = "123456"
        #expect(viewModel.canSubmit == true)
    }

    @Test("login view model sends otp with canonical login parameters")
    func testSendOtpUsesCanonicalParameters() async {
        let repository = MockAuthRepository()
        repository.sendOtpResult = .success(
            SendOtpResult(requestId: "otp-001", cooldownSeconds: 60, expiresInSeconds: 300)
        )
        let viewModel = LoginViewModel(
            sendOtpUseCase: SendOtpUseCase(repository: repository),
            createSessionUseCase: CreateSessionUseCase(repository: repository),
            onLoginSuccess: { _ in }
        )

        viewModel.phone = "13800138000"
        viewModel.hasAcceptedAgreement = true
        await viewModel.sendOtp()

        #expect(repository.sendOtpCallCount == 1)
        #expect(repository.lastSendOtpPhone == "13800138000")
        #expect(repository.lastSendOtpCountryCode == "+86")
        #expect(repository.lastSendOtpScene == "login")
        #expect(viewModel.viewState == .otpSent(cooldownRemaining: 60))
    }

    @Test("login view model blocks submit when agreement is unchecked")
    func testSubmitRequiresAgreement() async {
        let repository = MockAuthRepository()
        let viewModel = LoginViewModel(
            sendOtpUseCase: SendOtpUseCase(repository: repository),
            createSessionUseCase: CreateSessionUseCase(repository: repository),
            onLoginSuccess: { _ in }
        )

        viewModel.phone = "13800138000"
        viewModel.code = "123456"
        let success = await viewModel.submit()

        #expect(success == false)
        #expect(viewModel.globalError == "请先阅读并同意用户协议与隐私政策")
        #expect(repository.createSessionCallCount == 0)
    }

    @Test("login view model maps invalid code error to field error")
    func testSubmitMapsInvalidCodeError() async {
        let repository = MockAuthRepository()
        repository.createSessionResult = .failure(
            APIError.business(statusCode: 400, businessCode: "AUTH_INVALID_CODE", message: "验证码错误，请重新输入")
        )
        let viewModel = LoginViewModel(
            sendOtpUseCase: SendOtpUseCase(repository: repository),
            createSessionUseCase: CreateSessionUseCase(repository: repository),
            onLoginSuccess: { _ in }
        )

        viewModel.phone = "13800138000"
        viewModel.code = "123456"
        viewModel.hasAcceptedAgreement = true
        let success = await viewModel.submit()

        #expect(success == false)
        #expect(viewModel.codeError == "验证码错误，请重新输入")
        #expect(viewModel.viewState == .error("验证码错误，请重新输入"))
    }

    @Test("login view model persists login via success callback")
    func testSubmitCallsLoginSuccessCallback() async {
        let repository = MockAuthRepository()
        var receivedSession: AuthSession?
        let viewModel = LoginViewModel(
            sendOtpUseCase: SendOtpUseCase(repository: repository),
            createSessionUseCase: CreateSessionUseCase(repository: repository),
            onLoginSuccess: { session in
                receivedSession = session
            }
        )

        viewModel.phone = "13800138000"
        viewModel.code = "123456"
        viewModel.hasAcceptedAgreement = true
        let success = await viewModel.submit()

        #expect(success == true)
        #expect(viewModel.viewState == .success)
        #expect(receivedSession?.accessToken == "access-token")
        #expect(repository.lastCreateSessionPhone == "13800138000")
        #expect(repository.lastCreateSessionCountryCode == "+86")
        #expect(repository.lastCreateSessionCode == "123456")
    }
}
