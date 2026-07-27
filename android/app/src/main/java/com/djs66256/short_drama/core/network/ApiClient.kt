package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.core.config.AppConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Singleton responsible for building and exposing the Retrofit [ApiService] instance.
 * Uses [AppConfig] for non-hardcoded base URL configuration.
 */
object ApiClient {

    private var isInitialized = false

    private lateinit var _config: AppConfig
    private val config: AppConfig get() = _config

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val contentType = "application/json".toMediaType()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (config.isDebug) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                },
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(normalizeApiBaseUrl(config.apiBaseUrl))
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    /**
     * Initializes the ApiClient with an [AppConfig] instance.
     * Must be called (via Hilt) before any network operations.
     */
    fun initialize(appConfig: AppConfig) {
        if (!isInitialized) {
            _config = appConfig
            isInitialized = true
        }
    }

    internal fun normalizeApiBaseUrl(rawBaseUrl: String): String {
        val trimmed = rawBaseUrl.trim().removeSuffix("/")
        val canonicalPath = when {
            trimmed.endsWith("/api") -> trimmed
            trimmed.endsWith("/api/v1") -> trimmed.removeSuffix("/v1")
            else -> "$trimmed/api"
        }
        return "$canonicalPath/"
    }
}
