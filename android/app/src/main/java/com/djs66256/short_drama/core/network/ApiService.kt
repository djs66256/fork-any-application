package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.EpisodeListResponseDto
import com.djs66256.short_drama.data.dto.PlayerProgressResponseDto
import com.djs66256.short_drama.data.dto.PlayerStartRequestDto
import com.djs66256.short_drama.data.dto.PlayerStartResponseDto
import com.djs66256.short_drama.data.dto.PlayerStopRequestDto
import com.djs66256.short_drama.data.dto.PlayerStopResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface defining all backend endpoints.
 * Uses kotlinx.serialization for request/response serialization.
 * All methods return raw DTO types — ApiResult wrapping is done in the DataSource layer.
 */
interface ApiService {

    @GET("health")
    suspend fun health(): Unit

    @GET("dramas")
    suspend fun getDramas(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): DramaListResponseDto

    @POST("dramas")
    suspend fun createDrama(@Body body: Map<String, String>): Unit

    @GET("dramas/{id}")
    suspend fun getDramaDetail(@Path("id") id: String): Unit

    @GET("dramas/{id}/episodes")
    suspend fun getDramaEpisodes(
        @Path("id") dramaId: String,
    ): EpisodeListResponseDto

    @GET("episodes/{id}")
    suspend fun getEpisodeDetail(@Path("id") id: String): Unit

    @GET("player/progress")
    suspend fun getPlaybackProgress(
        @Header("X-Playback-Session-Id") playbackSessionId: String,
        @Query("dramaId") dramaId: String,
    ): PlayerProgressResponseDto

    @POST("player/start")
    suspend fun startPlayback(
        @Header("X-Playback-Session-Id") playbackSessionId: String,
        @Body body: PlayerStartRequestDto,
    ): PlayerStartResponseDto

    @POST("player/stop")
    suspend fun stopPlayback(
        @Header("X-Playback-Session-Id") playbackSessionId: String,
        @Body body: PlayerStopRequestDto,
    ): PlayerStopResponseDto
}
