package com.djs66256.short_drama.core.config

/**
 * Interface for accessing build-time configuration values.
 * Implemented via BuildConfig in the main source set,
 * can be mocked in unit tests.
 */
interface AppConfig {
    val isDebug: Boolean
    val apiBaseUrl: String
    val appName: String
    val appVersion: String
}
