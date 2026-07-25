package com.djs66256.short_drama.navigation

import android.net.Uri
import java.net.URI

object DeeplinkRouteParser {
    private const val EXPECTED_SCHEME = "djsdrama"

    fun parse(uri: Uri?): PendingRoute? = parse(uri?.toString())

    fun parse(rawUri: String?): PendingRoute? {
        val normalizedUri = rawUri?.trim().orEmpty()
        if (normalizedUri.isEmpty()) {
            return null
        }

        val uri = runCatching { URI(normalizedUri) }.getOrNull() ?: return null
        if (uri.scheme != EXPECTED_SCHEME) {
            return null
        }

        val segment = uri.path
            ?.trim('/')
            ?.substringBefore('/')
            ?.trim()
            .orEmpty()

        return when (uri.host) {
            "open" -> PendingRoute.Home
            "play", "player" -> {
                if (segment.isEmpty()) {
                    null
                } else {
                    PendingRoute.Play(segment)
                }
            }
            "drama" -> {
                if (segment.isEmpty()) {
                    null
                } else {
                    PendingRoute.Detail(segment)
                }
            }
            else -> null
        }
    }
}
