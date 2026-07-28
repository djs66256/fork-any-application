package com.djs66256.short_drama.core.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthAuthenticator @Inject constructor(
    private val authSessionProvider: com.djs66256.short_drama.domain.repository.AuthSessionProvider,
    private val refreshCoordinator: AuthRefreshCoordinator,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (!response.request.requiresAuth()) {
            return null
        }
        if (responseCount(response) >= MAX_AUTH_RETRY_COUNT) {
            return null
        }

        val authorizationHeader = response.request.header(AUTHORIZATION_HEADER)
        val currentAccessToken = authSessionProvider.accessToken().orEmpty()
        if (currentAccessToken.isBlank()) {
            return null
        }
        val currentBearer = bearerToken(currentAccessToken)
        if (authorizationHeader != null && authorizationHeader != currentBearer) {
            return response.request.newBuilder()
                .header(AUTHORIZATION_HEADER, currentBearer)
                .build()
        }

        return when (val refreshResult = refreshCoordinator.refreshBlocking()) {
            is ApiResult.Success -> {
                response.request.newBuilder()
                    .header(AUTHORIZATION_HEADER, bearerToken(refreshResult.data.accessToken))
                    .build()
            }
            is ApiResult.Error -> null
            is ApiResult.Exception -> null
        }
    }

    private fun responseCount(response: Response): Int {
        var currentResponse: Response? = response
        var count = 1
        while (currentResponse?.priorResponse != null) {
            count += 1
            currentResponse = currentResponse.priorResponse
        }
        return count
    }

    private fun bearerToken(token: String): String = "Bearer $token"

    private companion object {
        const val MAX_AUTH_RETRY_COUNT = 2
        const val AUTHORIZATION_HEADER = "Authorization"
    }
}
