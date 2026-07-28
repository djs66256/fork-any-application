package com.djs66256.short_drama.domain.model

enum class TheaterChannel(
    val apiValue: String,
    val label: String,
) {
    ALL(apiValue = "all", label = "找剧"),
    REAL(apiValue = "real", label = "真人"),
    ANIME(apiValue = "anime", label = "动漫"),
    MOVIE(apiValue = "movie", label = "电影"),
    AUDIO(apiValue = "audio", label = "有声书"),
    NOVEL(apiValue = "novel", label = "小说"),
    COMIC(apiValue = "comic", label = "漫画"),
    BIGSCREEN(apiValue = "bigscreen", label = "大屏"),
    ;

    companion object {
        fun fromApiValue(value: String?): TheaterChannel = entries.firstOrNull {
            it.apiValue == value.orEmpty().trim()
        } ?: ALL
    }
}
