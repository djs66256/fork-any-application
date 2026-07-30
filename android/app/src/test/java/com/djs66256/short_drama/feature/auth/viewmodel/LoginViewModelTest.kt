package com.djs66256.short_drama.feature.auth.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.AuthCooldownStore
import com.djs66256.short_drama.data.dto.SendOtpResult
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.usecase.CreateSessionUseCase
import com.djs66256.short_drama.domain.usecase.SendOtpUseCase
import com.djs66256.short_drama.navigation.AppDestination
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val sendOtpUseCase = mockk<SendOtpUseCase>()
    private val createSessionUseCase = mockk<CreateSessionUseCase>()
    private val authCooldownStore = mockk<AuthCooldownStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authCooldownStore.read() } returns null
        coEvery { authCooldownStore.write(any()) } returns Unit
        coEvery { authCooldownStore.clear() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-04 submit blocks when agreement not accepted`() = runTest {
        val viewModel = LoginViewModel(
            SavedStateHandle(),
            sendOtpUseCase,
            createSessionUseCase,
            authCooldownStore,
        )
        runCurrent()

        viewModel.onPhoneChange("13800138000")
        viewModel.onCodeChange("123456")
        viewModel.submitLogin()

        assertEquals("请先同意用户协议与隐私政策", viewModel.uiState.value.globalError)
        coVerify(exactly = 0) { createSessionUseCase.invoke(any(), any(), any()) }
    }

    @Test
    fun `T-05 send otp success enters cooldown and persists deadline`() = runTest {
        coEvery {
            sendOtpUseCase.invoke("+86", "13800138000", "login")
        } returns ApiResult.Success(
            SendOtpResult(
                requestId = "otp_req_1",
                cooldownSeconds = 60,
                expiresInSeconds = 300,
            ),
        )

        val viewModel = LoginViewModel(
            SavedStateHandle(),
            sendOtpUseCase,
            createSessionUseCase,
            authCooldownStore,
        )
        runCurrent()

        viewModel.onAgreementCheckedChange(true)
        viewModel.onPhoneChange("13800138000")
        viewModel.sendOtp()
        runCurrent()

        assertTrue(viewModel.uiState.value.cooldownRemainingSeconds in 59..60)
        assertNull(viewModel.uiState.value.globalError)
        coVerify(exactly = 1) { authCooldownStore.write(any()) }
    }

    @Test
    fun `T-06 login success emits success effect with return route`() = runTest {
        coEvery {
            createSessionUseCase.invoke("+86", "13800138000", "123456")
        } returns ApiResult.Success(sampleSession())

        val viewModel = LoginViewModel(
            SavedStateHandle(
                mapOf(AppDestination.Arg.RETURN_ROUTE to "ranking?contentType=all&type=booking"),
            ),
            sendOtpUseCase,
            createSessionUseCase,
            authCooldownStore,
        )
        runCurrent()
        viewModel.onAgreementCheckedChange(true)
        viewModel.onPhoneChange("13800138000")
        viewModel.onCodeChange("123456")

        viewModel.events.test {
            viewModel.submitLogin()
            advanceUntilIdle()
            assertEquals(
                LoginEvent.LoginSucceeded("ranking?contentType=all&type=booking"),
                awaitItem(),
            )
        }

        coVerify(exactly = 1) { authCooldownStore.clear() }
    }

    @Test
    fun `T-06 login success falls back to profile when return route points to login`() = runTest {
        coEvery {
            createSessionUseCase.invoke("+86", "13800138000", "123456")
        } returns ApiResult.Success(sampleSession())

        val viewModel = LoginViewModel(
            SavedStateHandle(
                mapOf(AppDestination.Arg.RETURN_ROUTE to "login?returnRoute=profile&source=profile"),
            ),
            sendOtpUseCase,
            createSessionUseCase,
            authCooldownStore,
        )
        runCurrent()
        viewModel.onAgreementCheckedChange(true)
        viewModel.onPhoneChange("13800138000")
        viewModel.onCodeChange("123456")

        viewModel.events.test {
            viewModel.submitLogin()
            advanceUntilIdle()
            assertEquals(
                LoginEvent.LoginSucceeded(AppDestination.Route.PROFILE),
                awaitItem(),
            )
        }
    }

    @Test
    fun `T-11 login success keeps menu booking as return route`() = runTest {
        coEvery {
            createSessionUseCase.invoke("+86", "13800138000", "123456")
        } returns ApiResult.Success(sampleSession())

        val viewModel = LoginViewModel(
            SavedStateHandle(
                mapOf(AppDestination.Arg.RETURN_ROUTE to AppDestination.menuBooking()),
            ),
            sendOtpUseCase,
            createSessionUseCase,
            authCooldownStore,
        )
        runCurrent()
        viewModel.onAgreementCheckedChange(true)
        viewModel.onPhoneChange("13800138000")
        viewModel.onCodeChange("123456")

        viewModel.events.test {
            viewModel.submitLogin()
            advanceUntilIdle()
            assertEquals(
                LoginEvent.LoginSucceeded(AppDestination.menuBooking()),
                awaitItem(),
            )
        }
    }

    @Test
    fun `T-06 submit login keeps original form values after request starts`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery {
            createSessionUseCase.invoke("+86", "13800138000", "123456")
        } coAnswers {
            gate.await()
            ApiResult.Success(sampleSession())
        }

        val viewModel = LoginViewModel(
            SavedStateHandle(),
            sendOtpUseCase,
            createSessionUseCase,
            authCooldownStore,
        )
        runCurrent()
        viewModel.onAgreementCheckedChange(true)
        viewModel.onPhoneChange("13800138000")
        viewModel.onCodeChange("123456")

        val submitJob = async { viewModel.submitLogin() }
        runCurrent()
        viewModel.onPhoneChange("13900139000")
        viewModel.onCodeChange("654321")
        gate.complete(Unit)
        submitJob.await()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            createSessionUseCase.invoke("+86", "13800138000", "123456")
        }
    }

    @Test
    fun `T-06 send otp is blocked while login submit is in flight`() = runTest {
        val loginGate = CompletableDeferred<Unit>()
        coEvery {
            createSessionUseCase.invoke("+86", "13800138000", "123456")
        } coAnswers {
            loginGate.await()
            ApiResult.Success(sampleSession())
        }

        val viewModel = LoginViewModel(
            SavedStateHandle(),
            sendOtpUseCase,
            createSessionUseCase,
            authCooldownStore,
        )
        runCurrent()
        viewModel.onAgreementCheckedChange(true)
        viewModel.onPhoneChange("13800138000")
        viewModel.onCodeChange("123456")

        viewModel.submitLogin()
        runCurrent()
        viewModel.sendOtp()
        loginGate.complete(Unit)
        advanceUntilIdle()

        coVerify(exactly = 0) { sendOtpUseCase.invoke(any(), any(), any()) }
    }

    private fun sampleSession(): AuthSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAtIso = "2026-07-28T12:34:56Z",
        user = AuthUser(
            id = "user-1",
            phone = "138****8000",
            displayName = null,
            avatarUrl = null,
            role = AuthRole.VIEWER,
            isNewUser = true,
        ),
    )
}
