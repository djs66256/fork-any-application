package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.domain.repository.AuthSessionProvider
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val authSessionProvider: AuthSessionProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (!originalRequest.requiresAuth()) {
            return chain.proceed(originalRequest)
        }

        val accessToken = authSessionProvider.accessToken().orEmpty()
        if (accessToken.isBlank()) {
            return chain.proceed(originalRequest)
        }

        val request = originalRequest.newBuilder()
            .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
            .build()
        return chain.proceed(request)
    }

    private fun bearerToken(token: String): String = "Bearer $token"

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}

internal fun Request.requiresAuth(): Boolean {
    val normalizedPath = url.encodedPath
        .removePrefix("/")
        .removePrefix("api/")

    return normalizedPath == "users/me" ||
        normalizedPath == "auth/session" ||
        normalizedPath == "dramas/rankings" ||
        normalizedPath == "check-ins/status" ||
        normalizedPath == "check-ins" ||
        normalizedPath == "messages/interactions" ||
        normalizedPath.matches(Regex("dramas/[^/]+/book"))
}
