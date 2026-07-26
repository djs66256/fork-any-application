package com.djs66256.short_drama.domain.model

enum class SeriesStatus(
    val wireValue: String,
    val label: String,
) {
    COMPLETED(wireValue = "completed", label = "已完结"),
    ONGOING(wireValue = "ongoing", label = "更新中"),
    ;

    companion object {
        fun fromWireValue(value: String?): SeriesStatus {
            return entries.firstOrNull { it.wireValue == value } ?: COMPLETED
        }
    }
}

data class DramaEpisodeList(
    val dramaId: String,
    val seriesStatus: SeriesStatus,
    val items: List<Episode>,
)

data class PlaybackProgress(
    val dramaId: String,
    val hasHistory: Boolean,
    val episodeId: String?,
    val startTime: Double,
    val updatedAt: String?,
)

data class StartPlaybackParams(
    val dramaId: String,
    val episodeId: String,
    val progress: Double,
)

data class StartPlaybackResult(
    val dramaId: String,
    val episodeId: String,
    val acceptedProgress: Double,
    val playbackSessionId: String,
    val startedAt: String,
)

data class StopPlaybackParams(
    val dramaId: String,
    val episodeId: String,
    val progress: Double,
    val duration: Double,
)

data class StopPlaybackResult(
    val dramaId: String,
    val episodeId: String,
    val savedProgress: Double,
    val duration: Double,
    val updatedAt: String,
)
