package com.djs66256.short_drama.feature.ranking.model

import com.djs66256.short_drama.domain.model.RankingDrama
import com.djs66256.short_drama.domain.model.RankingType
import kotlin.math.roundToInt

enum class RankingMetricVisual {
    FLAME,
    CALENDAR,
}

enum class RankingDetailTagTone {
    WARM,
    MINT,
    CORAL,
}

data class RankingDetailTagUiModel(
    val text: String,
    val tone: RankingDetailTagTone = RankingDetailTagTone.WARM,
)

enum class RankingPosterStyle {
    SUNSET,
    EMERALD,
    RIVIERA,
    VIOLET,
    BLUSH,
    MIDNIGHT,
    SCARLET,
    FOREST,
    SKY,
    PLUM,
    AMBER,
    PEARL,
    ;

    companion object {
        fun fromRank(rank: Int): RankingPosterStyle = when (rank % 6) {
            1 -> SUNSET
            2 -> EMERALD
            3 -> RIVIERA
            4 -> VIOLET
            5 -> BLUSH
            else -> MIDNIGHT
        }
    }
}

data class RankingDramaItemUiModel(
    val id: String,
    val rank: Int,
    val title: String,
    val secondaryText: String,
    val description: String,
    val coverUrl: String,
    val metricLabel: String,
    val metricValue: String,
    val metricVisual: RankingMetricVisual,
    val detailTags: List<RankingDetailTagUiModel>,
    val bookingHintText: String?,
    val bookingCount: Int,
    val isBooked: Boolean,
    val posterTitle: String,
    val posterStyle: RankingPosterStyle,
)

fun RankingDrama.toUiModel(
    rank: Int,
    rankingType: RankingType,
): RankingDramaItemUiModel {
    val metric = when (rankingType) {
        RankingType.HOT -> Triple("热度", formatCompactCount(playCount), RankingMetricVisual.FLAME)
        RankingType.RECOMMEND -> Triple(
            "推荐",
            formatCompactCount(recommendationScore.toRecommendationMetric()),
            RankingMetricVisual.FLAME,
        )
        RankingType.BOOKING -> Triple("预约数", formatCompactCount(bookingCount), RankingMetricVisual.CALENDAR)
    }

    return RankingDramaItemUiModel(
        id = id,
        rank = rank,
        title = title,
        secondaryText = buildRankingSecondaryText(category = category, tags = tags),
        description = description,
        coverUrl = coverUrl,
        metricLabel = metric.first,
        metricValue = metric.second,
        metricVisual = metric.third,
        detailTags = buildRankingDetailTags(
            rankingType = rankingType,
            playCount = playCount,
            bookingCount = bookingCount,
            recommendationScore = recommendationScore,
            rating = rating,
            episodeCount = episodeCount,
        ),
        bookingHintText = buildBookingHintText(
            rankingType = rankingType,
            bookingCount = bookingCount,
            episodeCount = episodeCount,
        ),
        bookingCount = bookingCount,
        isBooked = isBooked,
        posterTitle = buildPosterTitle(title),
        posterStyle = RankingPosterStyle.fromRank(rank),
    )
}

internal fun buildRankingMetaText(
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
            add("评分 ${formatMetricScore(rating)}")
        }
    }
    return parts.joinToString(" · ")
}

internal fun buildRankingSecondaryText(
    category: String,
    tags: List<String>,
): String {
    val parts = buildList {
        if (category.isNotBlank()) {
            add(category)
        }
        addAll(tags.take(2).filter { it.isNotBlank() })
    }
    return parts.joinToString(" · ")
}

internal fun buildRankingDetailTags(
    rankingType: RankingType,
    playCount: Int,
    bookingCount: Int,
    recommendationScore: Double,
    rating: Double,
    episodeCount: Int,
): List<RankingDetailTagUiModel> {
    val tags = mutableListOf<RankingDetailTagUiModel>()

    if (episodeCount in 1..16) {
        tags += RankingDetailTagUiModel(
            text = "新剧",
            tone = RankingDetailTagTone.MINT,
        )
    }
    if (rating >= 9.0) {
        tags += RankingDetailTagUiModel(
            text = "评分${formatMetricScore(rating)}",
            tone = RankingDetailTagTone.CORAL,
        )
    }

    when (rankingType) {
        RankingType.HOT -> {
            tags += RankingDetailTagUiModel(text = "${formatCompactCount((playCount * 0.05f).roundToInt())}收藏")
            tags += RankingDetailTagUiModel(text = "${formatCompactCount((playCount * 0.17f).roundToInt())}次点赞")
        }
        RankingType.RECOMMEND -> {
            tags += RankingDetailTagUiModel(text = "${formatCompactCount(playCount)}热度")
            tags += RankingDetailTagUiModel(text = "${formatCompactCount(recommendationScore.toLikeMetric())}次点赞")
        }
        RankingType.BOOKING -> {
            if (bookingCount > 0) {
                tags += RankingDetailTagUiModel(text = "${formatCompactCount(bookingCount)}人预约")
            }
        }
    }

    return tags.take(4)
}

internal fun buildBookingHintText(
    rankingType: RankingType,
    bookingCount: Int,
    episodeCount: Int,
): String? {
    if (rankingType != RankingType.BOOKING) {
        return null
    }
    val expectedMonth = ((episodeCount.coerceAtLeast(1) - 1) % 12) + 1
    return "预告 · ${formatCompactCount(bookingCount)}人预约 · 预计${expectedMonth}月上线"
}

internal fun buildPosterTitle(title: String): String {
    val compactTitle = title.replace(Regex("[，。！？、·《》：:（）()\\s]+"), "")
    if (compactTitle.isBlank()) {
        return "短剧"
    }
    return compactTitle.chunked(4).take(3).joinToString("\n")
}

internal fun formatMetricScore(value: Double): String = "%.1f".format(value)

internal fun formatCompactCount(value: Int): String {
    if (value >= 10000) {
        val wan = value / 10000.0
        val formatted = if (value % 10000 == 0) {
            "%.0f".format(wan)
        } else {
            "%.1f".format(wan).removeSuffix(".0")
        }
        return "${formatted}万"
    }
    return value.toString()
}

private fun Double.toRecommendationMetric(): Int = (this * 100000).roundToInt().coerceAtLeast(1)

private fun Double.toLikeMetric(): Int = (this * 10000).roundToInt().coerceAtLeast(1)
