package com.djs66256.short_drama.core.config

import com.djs66256.short_drama.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [AppConfig] backed by [BuildConfig].
 */
@Singleton
class BuildConfigAppConfig @Inject constructor() : AppConfig {
    override val isDebug: Boolean get() = BuildConfig.DEBUG
    override val apiBaseUrl: String get() = BuildConfig.API_BASE_URL
    override val mallBaseUrl: String get() = BuildConfig.MALL_BASE_URL
    override val appName: String get() = BuildConfig.APP_NAME
    override val appVersion: String get() = BuildConfig.APP_VERSION
}
