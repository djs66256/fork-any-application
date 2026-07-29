package com.djs66256.short_drama.domain.model

private const val DEFAULT_COMMENT_PAGE = 1
private const val DEFAULT_COMMENT_PAGE_SIZE = 20

enum class CommentSort(
    val apiValue: String,
    val label: String,
) {
    LATEST(apiValue = "latest", label = "最新"),
    HOT(apiValue = "hot", label = "最热"),
    ;

    companion object {
        fun fromApiValue(value: String?): CommentSort {
            return entries.firstOrNull { it.apiValue == value.orEmpty().trim() } ?: LATEST
        }
    }
}

data class CommentQuery(
    val dramaId: String,
    val page: Int = DEFAULT_COMMENT_PAGE,
    val pageSize: Int = DEFAULT_COMMENT_PAGE_SIZE,
    val sort: CommentSort = CommentSort.LATEST,
)

data class CommentPage(
    val dramaId: String,
    val items: List<Comment>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
) {
    val hasNextPage: Boolean = page < totalPages
}
