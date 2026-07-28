package com.djs66256.short_drama.core.auth

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.usecase.GetCurrentUserUseCase
import com.djs66256.short_drama.domain.usecase.RefreshSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthBootstrapperTest {
    private val sessionStore = FakeBootstrapAuthSessionStore()
    private val authStateHolder = AuthStateHolder(sessionStore)
    private val getCurrentUserUseCase = mockk<GetCurrentUserUseCase>()
    private val refreshSessionUseCase = mockk<RefreshSessionUseCase>()

    @Test
    fun `T-09 restore refreshes expired session and keeps authenticated state`() = runTest {
        val localSession = sampleSession(refreshToken = "refresh-token")
        val refreshedSession = sampleSession(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            displayName = "刷新后用户",
            isNewUser = false,
        )
        sessionStore.write(localSession)
        coEvery { getCurrentUserUseCase.invoke() } returns ApiResult.Error(
            code = "AUTH_UNAUTHORIZED",
            message = "token expired",
        )
        coEvery { refreshSessionUseCase.invoke("refresh-token") } coAnswers {
            authStateHolder.updateSession(refreshedSession)
            ApiResult.Success(refreshedSession)
        }

        val bootstrapper = AuthBootstrapper(
            authStateHolder = authStateHolder,
            getCurrentUserUseCase = getCurrentUserUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
        )

        bootstrapper.restoreIfNeeded()

        assertEquals(AuthStatus.Authenticated(refreshedSession), authStateHolder.authStatus.value)
        coVerify(exactly = 1) { refreshSessionUseCase.invoke("refresh-token") }
    }

    @Test
    fun `T-09 restore clears local session when unauthorized and refresh token missing`() = runTest {
        val localSession = sampleSession(refreshToken = "")
        sessionStore.write(localSession)
        coEvery { getCurrentUserUseCase.invoke() } returns ApiResult.Error(
            code = "AUTH_UNAUTHORIZED",
            message = "token expired",
        )
        coEvery { refreshSessionUseCase.invoke(any()) } returns ApiResult.Exception(IllegalStateException())

        val bootstrapper = AuthBootstrapper(
            authStateHolder = authStateHolder,
            getCurrentUserUseCase = getCurrentUserUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
        )

        bootstrapper.restoreIfNeeded()

        assertEquals(AuthStatus.Anonymous, authStateHolder.authStatus.value)
        assertEquals(null, sessionStore.read())
        coVerify(exactly = 0) { refreshSessionUseCase.invoke(any()) }
    }

    @Test
    fun `T-09 restore clears local session when refresh hits transient exception`() = runTest {
        val localSession = sampleSession(refreshToken = "refresh-token")
        sessionStore.write(localSession)
        coEvery { getCurrentUserUseCase.invoke() } returns ApiResult.Error(
            code = "AUTH_UNAUTHORIZED",
            message = "token expired",
        )
        coEvery { refreshSessionUseCase.invoke("refresh-token") } returns ApiResult.Exception(
            IllegalStateException("network down"),
        )

        val bootstrapper = AuthBootstrapper(
            authStateHolder = authStateHolder,
            getCurrentUserUseCase = getCurrentUserUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
        )

        bootstrapper.restoreIfNeeded()

        assertEquals(AuthStatus.Anonymous, authStateHolder.authStatus.value)
        assertEquals(null, sessionStore.read())
        coVerify(exactly = 1) { refreshSessionUseCase.invoke("refresh-token") }
    }

    @Test
    fun `T-09 restore clears local session when refresh returns server error`() = runTest {
        val localSession = sampleSession(refreshToken = "refresh-token")
        sessionStore.write(localSession)
        coEvery { getCurrentUserUseCase.invoke() } returns ApiResult.Error(
            code = "AUTH_UNAUTHORIZED",
            message = "token expired",
        )
        coEvery { refreshSessionUseCase.invoke("refresh-token") } returns ApiResult.Error(
            code = "HTTP_503",
            message = "service unavailable",
        )

        val bootstrapper = AuthBootstrapper(
            authStateHolder = authStateHolder,
            getCurrentUserUseCase = getCurrentUserUseCase,
            refreshSessionUseCase = refreshSessionUseCase,
        )

        bootstrapper.restoreIfNeeded()

        assertEquals(AuthStatus.Anonymous, authStateHolder.authStatus.value)
        assertEquals(null, sessionStore.read())
        coVerify(exactly = 1) { refreshSessionUseCase.invoke("refresh-token") }
    }

    private fun sampleSession(
        accessToken: String = "access-token",
        refreshToken: String = "refresh-token",
        displayName: String? = "测试用户",
        isNewUser: Boolean = true,
    ): AuthSession = AuthSession(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtIso = "2026-07-28T13:34:56Z",
        user = AuthUser(
            id = "user-1",
            phone = "138****8000",
            displayName = displayName,
            avatarUrl = null,
            role = AuthRole.VIEWER,
            isNewUser = isNewUser,
        ),
    )
}

private class FakeBootstrapAuthSessionStore : AuthSessionStore {
    private var session: AuthSession? = null

    override suspend fun read(): AuthSession? = session

    override suspend fun write(session: AuthSession) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}
