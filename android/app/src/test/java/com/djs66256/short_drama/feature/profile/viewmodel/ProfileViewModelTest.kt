package com.djs66256.short_drama.feature.profile.viewmodel

import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val sessionStore = FakeProfileAuthSessionStore()
    private val authStateHolder = AuthStateHolder(sessionStore)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `T-07 anonymous status maps to anonymous profile state`() = runTest {
        val viewModel = ProfileViewModel(authStateHolder)
        advanceUntilIdle()

        assertEquals(ProfileUiState.Anonymous, viewModel.uiState.value)
    }

    @Test
    fun `T-07 restoring status maps to loading profile state`() = runTest {
        val viewModel = ProfileViewModel(authStateHolder)

        authStateHolder.markRefreshing()
        advanceUntilIdle()

        assertEquals(ProfileUiState.Restoring, viewModel.uiState.value)
    }

    @Test
    fun `T-07 authenticated status exposes masked phone summary`() = runTest {
        val viewModel = ProfileViewModel(authStateHolder)
        authStateHolder.updateSession(sampleSession())
        advanceUntilIdle()

        assertEquals(
            ProfileUiState.Authenticated(
                maskedPhone = "138****8000",
                displayName = "测试用户",
                isNewUser = true,
            ),
            viewModel.uiState.value,
        )
    }

    private fun sampleSession(): AuthSession = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresAtIso = "2026-07-28T13:34:56Z",
        user = AuthUser(
            id = "user-1",
            phone = "138****8000",
            displayName = "测试用户",
            avatarUrl = null,
            role = AuthRole.VIEWER,
            isNewUser = true,
        ),
    )
}

private class FakeProfileAuthSessionStore : AuthSessionStore {
    private var session: AuthSession? = null

    override suspend fun read(): AuthSession? = session

    override suspend fun write(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}
