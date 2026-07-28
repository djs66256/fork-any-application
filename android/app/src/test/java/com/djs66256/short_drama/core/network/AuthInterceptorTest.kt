package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthInterceptorTest {
    private val authSessionProvider = mockk<AuthSessionProvider>()
    private val interceptor = AuthInterceptor(authSessionProvider)

    @Test
    fun `T-03 interceptor adds bearer header to protected request`() {
        every { authSessionProvider.accessToken() } returns "access-token"
        val chain = RecordingChain(request("https://example.com/api/users/me"))

        interceptor.intercept(chain)

        assertEquals("Bearer access-token", chain.proceededRequest.header("Authorization"))
    }

    @Test
    fun `T-03 interceptor skips anonymous auth endpoints`() {
        every { authSessionProvider.accessToken() } returns "access-token"
        val chain = RecordingChain(request("https://example.com/api/auth/session-refreshes"))

        interceptor.intercept(chain)

        assertEquals(null, chain.proceededRequest.header("Authorization"))
    }

    @Test
    fun `T-03 interceptor leaves request unchanged when token missing`() {
        every { authSessionProvider.accessToken() } returns null
        val chain = RecordingChain(request("https://example.com/api/dramas/rankings?type=booking&contentType=all"))

        interceptor.intercept(chain)

        assertEquals(null, chain.proceededRequest.header("Authorization"))
    }
}

private class RecordingChain(
    private val initialRequest: Request,
) : Interceptor.Chain {
    lateinit var proceededRequest: Request

    override fun request(): Request = initialRequest

    override fun proceed(request: Request): Response {
        proceededRequest = request
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()
    }

    override fun connection() = null
    override fun call() = throw UnsupportedOperationException()
    override fun connectTimeoutMillis(): Int = 0
    override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    override fun readTimeoutMillis(): Int = 0
    override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    override fun writeTimeoutMillis(): Int = 0
    override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
}

private fun request(url: String): Request = Request.Builder().url(url).build()
