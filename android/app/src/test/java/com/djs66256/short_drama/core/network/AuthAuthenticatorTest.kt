package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.domain.model.AuthRole
import com.djs66256.short_drama.domain.model.AuthSession
import com.djs66256.short_drama.domain.model.AuthUser
import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthAuthenticatorTest {
    private val authSessionProvider = mockk<AuthSessionProvider>()
    private val refreshCoordinator = mockk<AuthRefreshCoordinator>()
    private val authenticator = AuthAuthenticator(authSessionProvider, refreshCoordinator)

    @Test
    fun `T-03 authenticator retries protected request with refreshed token`() {
        every { authSessionProvider.accessToken() } returns "expired-token" andThen "refreshed-token"
        every { refreshCoordinator.refreshBlocking() } returns ApiResult.Success(sampleSession("refreshed-token"))

        val retriedRequest = authenticator.authenticate(
            null,
            unauthorizedResponse(request("https://example.com/api/users/me"), "Bearer expired-token"),
        )

        assertEquals("Bearer refreshed-token", retriedRequest?.header("Authorization"))
    }

    @Test
    fun `T-03 authenticator stops after single retry chain`() {
        every { authSessionProvider.accessToken() } returns "expired-token"

        val first = unauthorizedResponse(request("https://example.com/api/users/me"), "Bearer expired-token")
        val second = unauthorizedResponse(
            request("https://example.com/api/users/me"),
            "Bearer expired-token",
            priorResponse = first,
        )

        val result = authenticator.authenticate(null, second)

        assertNull(result)
    }

    @Test
    fun `T-03 authenticator skips anonymous endpoints`() {
        val result = authenticator.authenticate(
            null,
            unauthorizedResponse(request("https://example.com/api/auth/session-refreshes"), null),
        )

        assertNull(result)
    }

    private fun sampleSession(accessToken: String): AuthSession = AuthSession(
        accessToken = accessToken,
        refreshToken = "refresh-token",
        expiresAtIso = "2026-07-28T13:34:56Z",
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

private fun request(url: String): Request = Request.Builder().url(url).build()

private fun unauthorizedResponse(
    request: Request,
    authorizationHeader: String?,
    priorResponse: Response? = null,
): Response {
    val actualRequest = request.newBuilder().apply {
        if (authorizationHeader != null) {
            header("Authorization", authorizationHeader)
        }
    }.build()

    return Response.Builder()
        .request(actualRequest)
        .protocol(Protocol.HTTP_1_1)
        .code(401)
        .message("Unauthorized")
        .priorResponse(priorResponse)
        .build()
}
