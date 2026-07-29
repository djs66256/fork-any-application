package com.djs66256.short_drama.feature.mall.viewmodel

import app.cash.turbine.test
import com.djs66256.short_drama.core.config.AppConfig
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import com.djs66256.short_drama.feature.mall.model.DEFAULT_MALL_ERROR_MESSAGE
import com.djs66256.short_drama.feature.mall.model.MallBridgeMessage
import com.djs66256.short_drama.feature.mall.model.MallHostAuthReason
import com.djs66256.short_drama.feature.mall.model.MallHostMessage
import com.djs66256.short_drama.feature.mall.model.MallLoginContext
import com.djs66256.short_drama.feature.mall.model.MallLoginResult
import com.djs66256.short_drama.feature.mall.model.MallPageEvent
import com.djs66256.short_drama.feature.mall.model.MallRestoreReason
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
class MallViewModelTest {
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
    fun `T-01 initial state uses config mall base url`() {
        val viewModel = createViewModel(
            appConfig = FakeAppConfig(mallBaseUrl = "https://mall.example.com"),
            authSessionProvider = FakeAuthSessionProvider(isLoggedIn = false),
        )

        val state = viewModel.uiState.value
        assertEquals("https://mall.example.com/mall", state.currentUrl)
        assertEquals(MallContainerState.Loading, state.state)
        assertNull(state.pendingLoginContext)
    }

    @Test
    fun `T-02 page lifecycle updates loading success error and retry`() = runTest {
        val viewModel = createViewModel()

        viewModel.onPageEvent(MallPageEvent.LoadStarted("https://mall.example.com/mall"))
        assertEquals(MallContainerState.Loading, viewModel.uiState.value.state)

        viewModel.onPageEvent(MallPageEvent.LoadSucceeded("https://mall.example.com/mall"))
        advanceUntilIdle()
        assertEquals(MallContainerState.Success, viewModel.uiState.value.state)
        assertEquals("https://mall.example.com/mall", viewModel.uiState.value.lastLoadedHomeUrl)

        viewModel.onPageEvent(MallPageEvent.LoadFailed(message = "network down"))
        assertEquals(MallContainerState.Error("network down"), viewModel.uiState.value.state)

        viewModel.retryLoadHome()
        val retriedState = viewModel.uiState.value
        assertEquals(MallContainerState.Loading, retriedState.state)
        assertEquals("https://mall.example.com/mall", retriedState.currentUrl)
    }

    @Test
    fun `T-03 open search bridge only emits effect for valid payload`() = runTest {
        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.onBridgeMessage(
                MallBridgeMessage.OpenSearch(
                    source = "mall",
                    returnTarget = "/mall",
                ),
            )
            assertEquals(MallEffect.OpenSearch, awaitItem())

            viewModel.onBridgeMessage(
                MallBridgeMessage.OpenSearch(
                    source = "home",
                    returnTarget = "/mall",
                ),
            )
            expectNoEvents()
        }
    }

    @Test
    fun `T-04 login bridge records pending context and ignores invalid or duplicate requests`() = runTest {
        val viewModel = createViewModel()
        val context = MallLoginContext(productId = "product-001")

        viewModel.effects.test {
            viewModel.onBridgeMessage(MallBridgeMessage.RequestLogin(context))
            assertEquals(MallEffect.OpenMallLogin(context), awaitItem())
            assertEquals(context, viewModel.uiState.value.pendingLoginContext)

            viewModel.onBridgeMessage(MallBridgeMessage.RequestLogin(context.copy(productId = "product-002")))
            expectNoEvents()
            assertEquals(context, viewModel.uiState.value.pendingLoginContext)
        }

        val invalidViewModel = createViewModel()
        invalidViewModel.onBridgeMessage(
            MallBridgeMessage.RequestLogin(
                MallLoginContext(productId = ""),
            ),
        )
        advanceUntilIdle()
        assertNull(invalidViewModel.uiState.value.pendingLoginContext)
    }

    @Test
    fun `T-05 search return login return app resume and container recreation emit host messages`() = runTest {
        val viewModel = createViewModel(authSessionProvider = FakeAuthSessionProvider(isLoggedIn = true))
        viewModel.onBridgeMessage(MallBridgeMessage.RequestLogin(MallLoginContext(productId = "product-001")))

        viewModel.effects.test {
            skipItems(1)

            viewModel.onSearchReturned()
            assertEquals(
                MallEffect.SendHostMessage(
                    MallHostMessage.SyncAuthState(
                        com.djs66256.short_drama.feature.mall.model.MallHostAuthState(
                            isLoggedIn = true,
                            reason = MallHostAuthReason.INITIAL_LOAD,
                        ),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                MallEffect.SendHostMessage(
                    MallHostMessage.RestoreContext(
                        com.djs66256.short_drama.feature.mall.model.MallRestoreContext(
                            reason = MallRestoreReason.SEARCH_RETURN,
                            preserveScroll = false,
                        ),
                    ),
                ),
                awaitItem(),
            )

            viewModel.onMallLoginResult(MallLoginResult.SUCCESS)
            assertNull(viewModel.uiState.value.pendingLoginContext)
            assertEquals(
                MallEffect.SendHostMessage(
                    MallHostMessage.SyncAuthState(
                        com.djs66256.short_drama.feature.mall.model.MallHostAuthState(
                            isLoggedIn = true,
                            reason = MallHostAuthReason.LOGIN_SUCCESS,
                        ),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                MallEffect.SendHostMessage(
                    MallHostMessage.RestoreContext(
                        com.djs66256.short_drama.feature.mall.model.MallRestoreContext(
                            reason = MallRestoreReason.LOGIN_RETURN,
                            preserveScroll = false,
                        ),
                    ),
                ),
                awaitItem(),
            )

            viewModel.onAppResumed()
            assertEquals(
                MallEffect.SendHostMessage(
                    MallHostMessage.SyncAuthState(
                        com.djs66256.short_drama.feature.mall.model.MallHostAuthState(
                            isLoggedIn = true,
                            reason = MallHostAuthReason.APP_RESUME,
                        ),
                    ),
                ),
                awaitItem(),
            )

            viewModel.onContainerRecreated()
            advanceUntilIdle()
            assertEquals(MallContainerState.Loading, viewModel.uiState.value.state)
            assertEquals("https://mall.example.com/mall", viewModel.uiState.value.currentUrl)
            assertEquals(
                MallEffect.SendHostMessage(
                    MallHostMessage.SyncAuthState(
                        com.djs66256.short_drama.feature.mall.model.MallHostAuthState(
                            isLoggedIn = true,
                            reason = MallHostAuthReason.APP_RESUME,
                        ),
                    ),
                ),
                awaitItem(),
            )
            assertEquals(
                MallEffect.SendHostMessage(
                    MallHostMessage.RestoreContext(
                        com.djs66256.short_drama.feature.mall.model.MallRestoreContext(
                            reason = MallRestoreReason.CONTAINER_RECREATED,
                            preserveScroll = false,
                        ),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `load failed uses default error when message blank`() {
        val viewModel = createViewModel()

        viewModel.onPageEvent(MallPageEvent.LoadFailed(message = ""))

        assertEquals(
            MallContainerState.Error(DEFAULT_MALL_ERROR_MESSAGE),
            viewModel.uiState.value.state,
        )
    }

    private fun createViewModel(
        appConfig: AppConfig = FakeAppConfig(),
        authSessionProvider: AuthSessionProvider = FakeAuthSessionProvider(isLoggedIn = false),
    ): MallViewModel {
        return MallViewModel(appConfig, authSessionProvider)
    }

    private data class FakeAppConfig(
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
        override fun currentSession(): com.djs66256.short_drama.domain.model.AuthSession? {
            if (!isLoggedIn) {
                return null
            }

            return com.djs66256.short_drama.domain.model.AuthSession(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresAtIso = "2026-07-28T12:34:56Z",
                user = com.djs66256.short_drama.domain.model.AuthUser(
                    id = "550e8400-e29b-41d4-a716-446655440001",
                    phone = "13800138000",
                    displayName = "Mall Tester",
                    avatarUrl = null,
                    role = com.djs66256.short_drama.domain.model.AuthRole.VIEWER,
                    isNewUser = false,
                ),
            )
        }
    }
}
