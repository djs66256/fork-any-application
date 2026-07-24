package com.djs66256.short_drama.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor for injecting authentication tokens into requests.
 * Currently a skeleton — reserved for future JWT token injection.
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // TODO: Inject JWT token when auth system is implemented
        // val token = tokenProvider.getToken()
        // val request = originalRequest.newBuilder()
        //     .header("Authorization", "Bearer $token")
        //     .build()

        return chain.proceed(originalRequest)
    }
}
