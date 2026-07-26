package com.djs66256.short_drama.data.dto

import com.djs66256.short_drama.domain.model.DramaEpisodeList
import com.djs66256.short_drama.domain.model.PlaybackProgress
import com.djs66256.short_drama.domain.model.SeriesStatus
import com.djs66256.short_drama.domain.model.StartPlaybackResult
import com.djs66256.short_drama.domain.model.StopPlaybackResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeListDataDto(
    @SerialName("drama_id")
    val dramaId: String,
    @SerialName("series_status")
    val seriesStatus: String = SeriesStatus.COMPLETED.wireValue,
    val items: List<EpisodeDto>,
)

@Serializable
data class EpisodeListResponseDto(
    val code: Int = 0,
    val data: EpisodeListDataDto,
    val message: String = "ok",
) {
    fun toDomain(): DramaEpisodeList = DramaEpisodeList(
        dramaId = data.dramaId,
        seriesStatus = SeriesStatus.fromWireValue(data.seriesStatus),
        items = data.items
            .sortedBy(EpisodeDto::episodeNumber)
            .map(EpisodeDto::toDomain),
    )
}

@Serializable
data class PlayerProgressDataDto(
    @SerialName("drama_id")
    val dramaId: String,
    @SerialName("has_history")
    val hasHistory: Boolean,
    @SerialName("episode_id")
    val episodeId: String? = null,
    @SerialName("start_time")
    val startTime: Double = 0.0,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
data class PlayerProgressResponseDto(
    val code: Int = 0,
    val data: PlayerProgressDataDto,
    val message: String = "ok",
) {
    fun toDomain(): PlaybackProgress = PlaybackProgress(
        dramaId = data.dramaId,
        hasHistory = data.hasHistory,
        episodeId = data.episodeId,
        startTime = data.startTime,
        updatedAt = data.updatedAt,
    )
}

@Serializable
data class PlayerStartRequestDto(
    @SerialName("drama_id")
    val dramaId: String,
    @SerialName("episode_id")
    val episodeId: String,
    val progress: Double,
)

@Serializable
data class PlayerStartDataDto(
    @SerialName("drama_id")
    val dramaId: String,
    @SerialName("episode_id")
    val episodeId: String,
    @SerialName("accepted_progress")
    val acceptedProgress: Double,
    @SerialName("playback_session_id")
    val playbackSessionId: String,
    @SerialName("started_at")
    val startedAt: String,
)

@Serializable
data class PlayerStartResponseDto(
    val code: Int = 0,
    val data: PlayerStartDataDto,
    val message: String = "ok",
) {
    fun toDomain(): StartPlaybackResult = StartPlaybackResult(
        dramaId = data.dramaId,
        episodeId = data.episodeId,
        acceptedProgress = data.acceptedProgress,
        playbackSessionId = data.playbackSessionId,
        startedAt = data.startedAt,
    )
}

@Serializable
data class PlayerStopRequestDto(
    @SerialName("drama_id")
    val dramaId: String,
    @SerialName("episode_id")
    val episodeId: String,
    val progress: Double,
    val duration: Double,
)

@Serializable
data class PlayerStopDataDto(
    @SerialName("drama_id")
    val dramaId: String,
    @SerialName("episode_id")
    val episodeId: String,
    @SerialName("saved_progress")
    val savedProgress: Double,
    val duration: Double,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class PlayerStopResponseDto(
    val code: Int = 0,
    val data: PlayerStopDataDto,
    val message: String = "ok",
) {
    fun toDomain(): StopPlaybackResult = StopPlaybackResult(
        dramaId = data.dramaId,
        episodeId = data.episodeId,
        savedProgress = data.savedProgress,
        duration = data.duration,
        updatedAt = data.updatedAt,
    )
}
