package com.djs66256.short_drama.feature.profile.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.domain.usecase.LogoutUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val logoutUseCase = mockk<LogoutUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-08 logout success emits completion`() = runTest {
        coEvery { logoutUseCase.invoke() } returns ApiResult.Success(Unit)
        val viewModel = SettingsViewModel(logoutUseCase)

        viewModel.events.test {
            viewModel.logout()
            advanceUntilIdle()
            assertEquals(SettingsEvent.LogoutCompleted, awaitItem())
            assertFalse(viewModel.uiState.value.isLogoutSubmitting)
        }

        coVerify(exactly = 1) { logoutUseCase.invoke() }
    }

    @Test
    fun `T-08 logout failure still completes with local-first semantics`() = runTest {
        coEvery { logoutUseCase.invoke() } returns ApiResult.Error(
            code = "SERVICE_UNAVAILABLE",
            message = "稍后重试",
        )
        val viewModel = SettingsViewModel(logoutUseCase)

        viewModel.events.test {
            viewModel.logout()
            advanceUntilIdle()
            assertEquals(SettingsEvent.ShowMessage("稍后重试"), awaitItem())
            assertEquals(SettingsEvent.LogoutCompleted, awaitItem())
            assertFalse(viewModel.uiState.value.isLogoutSubmitting)
        }
    }
}
