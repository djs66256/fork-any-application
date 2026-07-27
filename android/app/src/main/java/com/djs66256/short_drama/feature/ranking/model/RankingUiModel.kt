package com.djs66256.short_drama.feature.ranking.model

import com.djs66256.short_drama.domain.model.RankingDrama
import com.djs66256.short_drama.domain.model.RankingType

data class RankingDramaItemUiModel(
    val id: String,
    val rank: Int,
    val title: String,
    val description: String,
    val coverUrl: String,
    val metaText: String,
    val metricLabel: String,
    val metricValue: String,
    val bookingCount: Int,
    val isBooked: Boolean,
)

fun RankingDrama.toUiModel(
    rank: Int,
    rankingType: RankingType,
): RankingDramaItemUiModel {
    val metric = when (rankingType) {
        RankingType.HOT -> "热度" to playCount.toString()
        RankingType.RECOMMEND -> "推荐值" to formatMetricScore(recommendationScore)
        RankingType.BOOKING -> "预约数" to bookingCount.toString()
    }

    return RankingDramaItemUiModel(
        id = id,
        rank = rank,
        title = title,
        description = description,
        coverUrl = coverUrl,
        metaText = buildRankingMetaText(
            category = category,
            tags = tags,
            episodeCount = episodeCount,
            rating = rating,
        ),
        metricLabel = metric.first,
        metricValue = metric.second,
        bookingCount = bookingCount,
        isBooked = isBooked,
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

internal fun formatMetricScore(value: Double): String = "%.1f".format(value)
