package com.djs66256.short_drama.core.network

object ApiClient {
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
