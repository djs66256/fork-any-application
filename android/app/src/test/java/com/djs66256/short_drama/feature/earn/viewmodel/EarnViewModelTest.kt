package com.djs66256.short_drama.feature.earn.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.feature.earn.model.DEFAULT_EARN_ERROR_MESSAGE
import com.djs66256.short_drama.feature.earn.model.EarnBridgeMessage
import com.djs66256.short_drama.feature.earn.model.EarnHostAuthReason
import com.djs66256.short_drama.feature.earn.model.EarnHostAuthState
import com.djs66256.short_drama.feature.earn.model.EarnHostMessage
import com.djs66256.short_drama.feature.earn.model.EarnLoginContext
import com.djs66256.short_drama.feature.earn.model.EarnLoginResult
import com.djs66256.short_drama.feature.earn.model.EarnPageEvent
import com.djs66256.short_drama.feature.earn.model.EarnRestoreContext
import com.djs66256.short_drama.feature.earn.model.EarnRestoreReason
import com.djs66256.short_drama.feature.earn.model.EarnTaskContext
import com.djs66256.short_drama.feature.earn.model.EarnTaskPlayerResult
import com.djs66256.short_drama.feature.earn.model.EarnTaskPlayerResultReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EarnViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-01 initial state uses config earn base url`() {
        val viewModel = createViewModel(
            appConfig = FakeAppConfig(earnBaseUrl = "https://earn.example.com"),
            authSessionProvider = FakeAuthSessionProvider(isLoggedIn = false),
        )

        val state = viewModel.uiState.value
        assertEquals("https://earn.example.com/earn", state.currentUrl)
        assertEquals(EarnContainerState.Loading, state.state)
        assertNull(state.pendingLoginContext)
        assertNull(state.pendingTaskContext)
    }

    @Test
    fun `T-02 page lifecycle updates loading success error and retry`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onPageEvent(EarnPageEvent.LoadStarted("https://earn.example.com/earn"))
            assertEquals(EarnContainerState.Loading, viewModel.uiState.value.state)

            viewModel.onPageEvent(EarnPageEvent.LoadSucceeded("https://earn.example.com/earn"))
            assertEquals(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.SyncAuthState(
                        EarnHostAuthState(
                            isLoggedIn = false,
                            reason = EarnHostAuthReason.INITIAL_LOAD,
                            apiAccessToken = null,
                            expiresAt = null,
                        ),
                    ),
                ),
                awaitItem(),
            )
            advanceUntilIdle()
            assertEquals(EarnContainerState.Success, viewModel.uiState.value.state)
            assertEquals("https://earn.example.com/earn", viewModel.uiState.value.lastLoadedHomeUrl)

            viewModel.onPageEvent(EarnPageEvent.LoadFailed(message = "network down"))
            assertEquals(EarnContainerState.Error("network down"), viewModel.uiState.value.state)

            viewModel.retryLoadHome()
            val retriedState = viewModel.uiState.value
            assertEquals(EarnContainerState.Loading, retriedState.state)
            assertEquals("https://earn.example.com/earn", retriedState.currentUrl)
        }
    }

    @Test
    fun `T-03 request login bridge records pending context and ignores invalid or duplicate requests`() = runTest {
        val viewModel = createViewModel()
        val context = EarnLoginContext()

        viewModel.effects.test {
            viewModel.onBridgeMessage(EarnBridgeMessage.RequestLogin(context))
            assertEquals(EarnEffect.OpenEarnLogin(context), awaitItem())
            assertEquals(context, viewModel.uiState.value.pendingLoginContext)

            viewModel.onBridgeMessage(EarnBridgeMessage.RequestLogin(context))
            expectNoEvents()
            assertEquals(context, viewModel.uiState.value.pendingLoginContext)
        }

        val invalidViewModel = createViewModel()
        invalidViewModel.onBridgeMessage(
            EarnBridgeMessage.RequestLogin(
                EarnLoginContext(source = "mall"),
            ),
        )
        advanceUntilIdle()
        assertNull(invalidViewModel.uiState.value.pendingLoginContext)
    }

    @Test
    fun `T-04 open task player bridge records pending context and ignores invalid or duplicate requests`() = runTest {
        val viewModel = createViewModel()
        val context = EarnTaskContext(
            taskId = "task-001",
            videoId = "video-001",
        )

        viewModel.effects.test {
            viewModel.onBridgeMessage(EarnBridgeMessage.OpenTaskPlayer(context))
            assertEquals(EarnEffect.OpenEarnTaskPlayer(context), awaitItem())
            assertEquals(context, viewModel.uiState.value.pendingTaskContext)

            viewModel.onBridgeMessage(
                EarnBridgeMessage.OpenTaskPlayer(
                    context.copy(taskId = "task-002"),
                ),
            )
            expectNoEvents()
            assertEquals(context, viewModel.uiState.value.pendingTaskContext)
        }

        val invalidViewModel = createViewModel()
        invalidViewModel.onBridgeMessage(
            EarnBridgeMessage.OpenTaskPlayer(
                EarnTaskContext(
                    taskId = "",
                    videoId = "video-001",
                ),
            ),
        )
        advanceUntilIdle()
        assertNull(invalidViewModel.uiState.value.pendingTaskContext)
    }

    @Test
    fun `T-05 login return syncs auth state then restores context`() = runTest {
        val viewModel = createViewModel(authSessionProvider = FakeAuthSessionProvider(isLoggedIn = true))
        viewModel.onBridgeMessage(EarnBridgeMessage.RequestLogin(EarnLoginContext()))

        viewModel.effects.test {
            skipItems(1)

            viewModel.onEarnLoginResult(EarnLoginResult.SUCCESS)
            assertNull(viewModel.uiState.value.pendingLoginContext)
            assertEquals(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.SyncAuthState(
                        EarnHostAuthState(
                            isLoggedIn = true,
                            reason = EarnHostAuthReason.LOGIN_SUCCESS,
                            apiAccessToken = "access-token",
                            expiresAt = "2026-07-29T12:34:56Z",
                        ),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.RestoreContext(
                        EarnRestoreContext(
                            reason = EarnRestoreReason.LOGIN_RETURN,
                            preserveScroll = false,
                        ),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `T-06 task player result only completes when completed is true`() = runTest {
        val viewModel = createViewModel()
        val context = EarnTaskContext(taskId = "task-001", videoId = "video-001")
        viewModel.onBridgeMessage(EarnBridgeMessage.OpenTaskPlayer(context))

        viewModel.effects.test {
            skipItems(1)

            viewModel.onEarnTaskPlayerResult(
                EarnTaskPlayerResult(
                    taskId = "task-001",
                    videoId = "video-001",
                    completed = true,
                    reason = EarnTaskPlayerResultReason.PLAYBACK_ENDED,
                ),
            )
            assertEquals(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.CompleteTask(
                        EarnTaskPlayerResult(
                            taskId = "task-001",
                            videoId = "video-001",
                            completed = true,
                            reason = EarnTaskPlayerResultReason.PLAYBACK_ENDED,
                        ),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.RestoreContext(
                        EarnRestoreContext(
                            reason = EarnRestoreReason.TASK_RETURN,
                            preserveScroll = false,
                        ),
                    ),
                ),
                awaitItem(),
            )
            assertNull(viewModel.uiState.value.pendingTaskContext)
        }

        val incompleteViewModel = createViewModel()
        incompleteViewModel.onBridgeMessage(EarnBridgeMessage.OpenTaskPlayer(context))
        incompleteViewModel.effects.test {
            skipItems(1)

            incompleteViewModel.onEarnTaskPlayerResult(
                EarnTaskPlayerResult(
                    taskId = "task-001",
                    videoId = "video-001",
                    completed = false,
                    reason = EarnTaskPlayerResultReason.USER_EXIT,
                ),
            )
            assertEquals(
                EarnEffect.SendHostMessage(
                    EarnHostMessage.RestoreContext(
                        EarnRestoreContext(
                            reason = EarnRestoreReason.TASK_RETURN,
                            preserveScroll = false,
                        ),
                    ),
                ),
                awaitItem(),
            )
            expectNoEvents()
            assertNull(incompleteViewModel.uiState.value.pendingTaskContext)
        }
    }

    @Test
    fun `blank load failure uses default error message`() {
        val viewModel = createViewModel()

        viewModel.onPageEvent(EarnPageEvent.LoadFailed(message = ""))

        assertEquals(
            EarnContainerState.Error(DEFAULT_EARN_ERROR_MESSAGE),
            viewModel.uiState.value.state,
        )
    }

    private fun createViewModel(
        appConfig: AppConfig = FakeAppConfig(),
        authSessionProvider: AuthSessionProvider = FakeAuthSessionProvider(isLoggedIn = false),
    ): EarnViewModel {
        return EarnViewModel(appConfig, authSessionProvider)
    }

    private data class FakeAppConfig(
        override val earnBaseUrl: String = "https://earn.example.com",
        override val mallBaseUrl: String = "https://mall.example.com",
    ) : AppConfig {
        override val isDebug: Boolean = true
        override val apiBaseUrl: String = "https://api.example.com/api/"
        override val appName: String = "ShortDrama"
        override val appVersion: String = "0.1.0"
    }

    private data class FakeAuthSessionProvider(
        private val isLoggedIn: Boolean,
    ) : AuthSessionProvider {
        override fun currentSession(): AuthSession? {
            if (!isLoggedIn) {
                return null
            }

            return AuthSession(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresAtIso = "2026-07-29T12:34:56Z",
                user = AuthUser(
                    id = "550e8400-e29b-41d4-a716-446655440001",
                    phone = "13800138000",
                    displayName = "Earn Tester",
                    avatarUrl = null,
                    role = AuthRole.VIEWER,
                    isNewUser = false,
                ),
            )
        }
    }
}
