package com.djs66256.short_drama.domain.model

private const val DEFAULT_PAGE = 1
private const val DEFAULT_PAGE_SIZE = 10

@Suppress("MagicNumber")
enum class RankingContentType(
    val apiValue: String,
    val label: String,
) {
    ALL(apiValue = "all", label = "全部"),
    LIVE_ACTION(apiValue = "live_action", label = "真人"),
    AI(apiValue = "ai", label = "AI"),
    ;

    companion object {
        fun fromApiValue(value: String?): RankingContentType = entries.firstOrNull {
            it.apiValue == value.orEmpty().trim()
        } ?: ALL
    }
}

enum class RankingType(
    val apiValue: String,
    val label: String,
) {
    HOT(apiValue = "hot", label = "热榜"),
    RECOMMEND(apiValue = "recommend", label = "推荐榜"),
    BOOKING(apiValue = "booking", label = "预约榜"),
    ;

    companion object {
        fun fromApiValue(value: String?): RankingType = entries.firstOrNull {
            it.apiValue == value.orEmpty().trim()
        } ?: HOT
    }
}

data class RankingQuery(
    val contentType: RankingContentType = RankingContentType.ALL,
    val type: RankingType = RankingType.HOT,
    val page: Int = DEFAULT_PAGE,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
)

data class RankingPage(
    val items: List<RankingDrama>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
) {
    val hasNextPage: Boolean = page < totalPages
}
