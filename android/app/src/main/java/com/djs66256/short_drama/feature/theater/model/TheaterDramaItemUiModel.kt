package com.djs66256.short_drama.feature.theater.model

import com.djs66256.short_drama.domain.model.TheaterDrama
import java.util.Locale
import kotlin.math.roundToInt

data class TheaterDramaItemUiModel(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val category: String,
    val chipTexts: List<String>,
    val badgeText: String?,
    val heatText: String,
)

fun TheaterDrama.toUiModel(): TheaterDramaItemUiModel = TheaterDramaItemUiModel(
    id = id,
    title = title,
    description = description,
    coverUrl = coverUrl,
    category = category,
    chipTexts = buildTheaterChipTexts(
        category = category,
        tags = tags,
        rating = rating,
        heat = heat,
    ),
    badgeText = buildBadgeText(
        title = title,
        tags = tags,
        heat = heat,
    ),
    heatText = formatHeat(heat),
)

internal fun buildTheaterChipTexts(
    category: String,
    tags: List<String>,
    rating: Double,
    heat: Int,
): List<String> {
    val chips = mutableListOf<String>()
    val highlight = when {
        heat >= 90_000 -> "热播榜 No.9"
        heat >= 80_000 -> "AI剧收藏榜 No.8"
        heat >= 60_000 -> "最高热度破9000万"
        else -> "红果首发"
    }
    chips += highlight
    val secondary = tags.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: category.takeIf { it.isNotBlank() }
        ?: if (rating > 0.0) "评分 ${formatRating(rating)}" else null
    secondary?.let(chips::add)
    return chips.take(2)
}

internal fun buildBadgeText(
    title: String,
    tags: List<String>,
    heat: Int,
): String? = when {
    heat >= 90_000 -> "爆剧"
    heat >= 80_000 -> "热播"
    title.contains("夜") -> "新剧"
    tags.any { it.contains("复仇") } -> "红果首发"
    else -> null
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
