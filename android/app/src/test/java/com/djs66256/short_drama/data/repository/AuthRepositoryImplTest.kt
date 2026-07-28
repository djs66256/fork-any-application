package com.djs66256.short_drama.data.repository

import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.data.datasource.AuthRemoteDataSource
import com.djs66256.short_drama.data.dto.ApiEnvelopeDto
import com.djs66256.short_drama.data.dto.AuthSessionPayloadDto
import com.djs66256.short_drama.data.dto.AuthUserDto
import com.djs66256.short_drama.data.dto.SendOtpPayloadDto
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.AuthUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryImplTest {
    private val remoteDataSource = mockk<AuthRemoteDataSource>()
    private val sessionStore = FakeAuthSessionStore()
    private val authStateHolder = AuthStateHolder(sessionStore)
    private val repository = AuthRepositoryImpl(remoteDataSource, authStateHolder)

    @Test
    fun `T-11 sendOtp maps envelope payload to domain result`() = runTest {
        coEvery {
            remoteDataSource.sendOtp("+86", "13800138000", "login")
        } returns ApiResult.Success(
            ApiEnvelopeDto(
                code = 0,
                data = SendOtpPayloadDto(
                    requestId = "otp_req_1",
                    cooldownSeconds = 60,
                    expiresInSeconds = 300,
                ),
                message = "ok",
            ),
        )

        val result = repository.sendOtp("+86", "13800138000", "login")

        assertEquals(ApiResult.Success(com.djs66256.short_drama.data.dto.SendOtpResult("otp_req_1", 60, 300)), result)
    }

    @Test
    fun `T-11 createSession updates auth state on success`() = runTest {
        val sessionPayload = sampleSessionPayload(accessToken = "new-access-token")
        coEvery {
            remoteDataSource.createSession("+86", "13800138000", "123456")
        } returns ApiResult.Success(
            ApiEnvelopeDto(code = 0, data = sessionPayload, message = "ok"),
        )

        val result = repository.createSession("+86", "13800138000", "123456")

        assertTrue(result is ApiResult.Success)
        assertEquals("new-access-token", (result as ApiResult.Success).data.accessToken)
        assertEquals("new-access-token", authStateHolder.accessToken())
        assertTrue(authStateHolder.authStatus.value is AuthStatus.Authenticated)
    }

    @Test
    fun `T-11 logout clears local session even when remote call fails`() = runTest {
        authStateHolder.updateSession(sampleSession())
        coEvery { remoteDataSource.logout() } returns ApiResult.Error(
            code = "SERVICE_UNAVAILABLE",
            message = "稍后重试",
        )

        val result = repository.logout()

        assertTrue(result is ApiResult.Error)
        assertEquals(null, authStateHolder.currentSession())
        assertEquals(null, sessionStore.persistedSession)
        assertEquals(AuthStatus.Anonymous, authStateHolder.authStatus.value)
        coVerify(exactly = 1) { remoteDataSource.logout() }
    }

    private fun sampleSessionPayload(accessToken: String): AuthSessionPayloadDto = AuthSessionPayloadDto(
        accessToken = accessToken,
        refreshToken = "refresh-token",
        expiresAt = "2026-07-28T13:34:56Z",
        user = AuthUserDto(
            id = "user-1",
            phone = "138****8000",
            displayName = null,
            avatarUrl = null,
            role = "viewer",
            isNewUser = false,
        ),
    )

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
            isNewUser = false,
        ),
    )
}

private class FakeAuthSessionStore : AuthSessionStore {
    var persistedSession: AuthSession? = null

    override suspend fun read(): AuthSession? = persistedSession

    override suspend fun write(session: AuthSession) {
        persistedSession = session
    }

    override suspend fun clear() {
        persistedSession = null
    }
}
