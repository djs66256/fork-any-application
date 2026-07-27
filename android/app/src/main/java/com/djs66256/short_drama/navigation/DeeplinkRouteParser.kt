package com.djs66256.short_drama.navigation

import android.net.Uri
import com.djs66256.short_drama.domain.model.normalizeSearchQueryOrNull
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

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

        val host = uri.host?.trim().orEmpty()
        val segments = uri.path
            ?.trim('/')
            ?.split('/')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        return when (host) {
            "open" -> PendingRoute.Home
            "play", "player" -> segments.firstOrNull()?.takeIf { it.isNotEmpty() }?.let(PendingRoute::Play)
            "drama" -> segments.firstOrNull()?.takeIf { it.isNotEmpty() }?.let(PendingRoute::Detail)
            "search" -> parseSearchRoute(uri, segments)
            "ranking" -> PendingRoute.Ranking
            "classification" -> PendingRoute.Classification
            "new-releases" -> PendingRoute.NewReleases
            "actors" -> PendingRoute.Actors
            else -> null
        }
    }

    private fun parseSearchRoute(uri: URI, segments: List<String>): PendingRoute? {
        if (segments.isEmpty()) {
            return PendingRoute.SearchHome
        }

        return if (segments.first() == "result") {
            val rawQuery = segments.drop(1).joinToString("/")
            decodeAndNormalize(rawQuery)?.let(PendingRoute::SearchResult)
        } else {
            null
        }
    }

    private fun decodeAndNormalize(rawQuery: String): String? {
        val decoded = runCatching {
            URLDecoder.decode(rawQuery, StandardCharsets.UTF_8.toString())
        }.getOrElse { rawQuery }
        return normalizeSearchQueryOrNull(decoded)
    }
}
