package com.djs66256.short_drama.feature.theater.model

import com.djs66256.short_drama.domain.model.TheaterDrama
import java.util.Locale
import kotlin.math.roundToInt

data class TheaterDramaItemUiModel(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val metaText: String,
    val heatText: String,
)

fun TheaterDrama.toUiModel(): TheaterDramaItemUiModel = TheaterDramaItemUiModel(
    id = id,
    title = title,
    description = description,
    coverUrl = coverUrl,
    metaText = buildTheaterMetaText(
        category = category,
        tags = tags,
        episodeCount = episodeCount,
        rating = rating,
    ),
    heatText = formatHeat(heat),
)

internal fun buildTheaterMetaText(
    category: String,
    tags: List<String>,
    episodeCount: Int,
    rating: Double,
): String {
    val parts = buildList {
        if (category.isNotBlank()) {
            add(category)
        }
        if (tags.isNotEmpty()) {
            add(tags.take(2).joinToString(" / "))
        }
        if (episodeCount > 0) {
            add("$episodeCount 集")
        }
        if (rating > 0.0) {
            add("评分 ${formatRating(rating)}")
        }
    }
    return parts.joinToString(" · ")
}

internal fun formatHeat(value: Int): String {
    if (value < TEN_THOUSAND) {
        return value.toString()
    }
    val tenThousands = value / TEN_THOUSAND.toDouble()
    val rounded = (tenThousands * 10).roundToInt() / 10.0
    val number = if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
    return "${number}万"
}

private fun formatRating(value: Double): String = String.format(Locale.US, "%.1f", value)

private const val TEN_THOUSAND = 10_000
