package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.storage.AuthSessionStore
import com.djs66256.short_drama.data.dto.ApiEnvelopeDto
import com.djs66256.short_drama.data.dto.AuthSessionPayloadDto
import com.djs66256.short_drama.data.dto.AuthUserDto
import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthStatus
import com.djs66256.short_drama.domain.model.AuthUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthRefreshCoordinatorTest {
    private val refreshApiService = mockk<ApiService>()
    private val sessionStore = FakeRefreshSessionStore(sampleSession())
    private val authStateHolder = AuthStateHolder(sessionStore)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `T-03 coordinator shares one refresh call for concurrent 401 handling`() = runBlocking {
        val enteredRefresh = CountDownLatch(1)
        val releaseRefresh = CountDownLatch(1)
        authStateHolder.updateSession(sampleSession())
        coEvery {
            refreshApiService.refreshAuthSession(any())
        } coAnswers {
            enteredRefresh.countDown()
            check(releaseRefresh.await(5, TimeUnit.SECONDS)) { "refresh call did not get released" }
            ApiEnvelopeDto(
                code = 0,
                data = sampleSessionPayload(accessToken = "new-access-token"),
                message = "ok",
            )
        }
        val coordinator = AuthRefreshCoordinator(refreshApiService, authStateHolder, json)

        val first = CompletableFuture.supplyAsync { coordinator.refreshBlocking() }
        check(enteredRefresh.await(5, TimeUnit.SECONDS)) { "first refresh did not start" }
        val second = CompletableFuture.supplyAsync { coordinator.refreshBlocking() }
        releaseRefresh.countDown()

        val firstResult = first.get(5, TimeUnit.SECONDS)
        val secondResult = second.get(5, TimeUnit.SECONDS)

        assertTrue(firstResult is ApiResult.Success)
        assertTrue(secondResult is ApiResult.Success)
        assertEquals("new-access-token", (firstResult as ApiResult.Success).data.accessToken)
        assertEquals("new-access-token", (secondResult as ApiResult.Success).data.accessToken)
        coVerify(exactly = 1) { refreshApiService.refreshAuthSession(any()) }
    }

    @Test
    fun `T-03 coordinator clears local session when refresh token is expired`() = runTest {
        authStateHolder.updateSession(sampleSession())
        val body = """{"error":{"code":"AUTH_REFRESH_EXPIRED","message":"登录态已失效，请重新登录"}}"""
            .toResponseBody("application/json".toMediaType())
        coEvery {
            refreshApiService.refreshAuthSession(any())
        } throws HttpException(Response.error<Any>(401, body))
        val coordinator = AuthRefreshCoordinator(refreshApiService, authStateHolder, json)

        val result = coordinator.refreshBlocking()

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("AUTH_REFRESH_EXPIRED", result.code)
        assertEquals(AuthStatus.Anonymous, authStateHolder.authStatus.value)
        assertEquals(null, sessionStore.persistedSession)
    }

    @Test
    fun `T-03 coordinator clears local session when refresh hits transient exception`() = runTest {
        authStateHolder.updateSession(sampleSession())
        coEvery {
            refreshApiService.refreshAuthSession(any())
        } throws IllegalStateException("network down")
        val coordinator = AuthRefreshCoordinator(refreshApiService, authStateHolder, json)

        val result = coordinator.refreshBlocking()

        assertTrue(result is ApiResult.Exception)
        assertEquals(AuthStatus.Anonymous, authStateHolder.authStatus.value)
        assertEquals(null, sessionStore.persistedSession)
    }

    private fun sampleSessionPayload(accessToken: String): AuthSessionPayloadDto = AuthSessionPayloadDto(
        accessToken = accessToken,
        refreshToken = "new-refresh-token",
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

private class FakeRefreshSessionStore(
    initialSession: AuthSession?,
) : AuthSessionStore {
    var persistedSession: AuthSession? = initialSession

    override suspend fun read(): AuthSession? = persistedSession

    override suspend fun write(session: AuthSession) {
        persistedSession = session
    }

    override suspend fun clear() {
        persistedSession = null
    }
}
