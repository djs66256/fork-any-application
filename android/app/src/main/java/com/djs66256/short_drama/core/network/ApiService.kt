package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.data.dto.ApiEnvelopeDto
import com.djs66256.short_drama.data.dto.AuthSessionPayloadDto
import com.djs66256.short_drama.data.dto.AuthUserDto
import com.djs66256.short_drama.data.dto.BookDramaResponseDto
import com.djs66256.short_drama.data.dto.ClassificationTagsResponseDto
import com.djs66256.short_drama.data.dto.CreateAuthSessionRequestDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.EpisodeListResponseDto
import com.djs66256.short_drama.data.dto.HotSearchListResponseDto
import com.djs66256.short_drama.data.dto.PlayerProgressResponseDto
import com.djs66256.short_drama.data.dto.PlayerStartRequestDto
import com.djs66256.short_drama.data.dto.PlayerStartResponseDto
import com.djs66256.short_drama.data.dto.PlayerStopRequestDto
import com.djs66256.short_drama.data.dto.PlayerStopResponseDto
import com.djs66256.short_drama.data.dto.RankingListResponseDto
import com.djs66256.short_drama.data.dto.RefreshAuthSessionRequestDto
import com.djs66256.short_drama.data.dto.SendOtpPayloadDto
import com.djs66256.short_drama.data.dto.SendOtpRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    @GET("dramas/search")
    suspend fun searchDramas(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): DramaListResponseDto

    @GET("dramas/hot-search")
    suspend fun getHotSearches(): HotSearchListResponseDto

    @GET("dramas/tags")
    suspend fun getDramaTags(
        @Query("gender") gender: String = "all",
    ): ClassificationTagsResponseDto

    @GET("dramas/rankings")
    suspend fun getDramaRankings(
        @Query("type") type: String,
        @Query("contentType") contentType: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 10,
    ): RankingListResponseDto

    @POST("dramas/{id}/book")
    suspend fun bookDrama(@Path("id") id: String): BookDramaResponseDto

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

    @POST("auth/otp-requests")
    suspend fun sendOtpRequest(
        @Body body: SendOtpRequestDto,
    ): ApiEnvelopeDto<SendOtpPayloadDto>

    @POST("auth/sessions")
    suspend fun createAuthSession(
        @Body body: CreateAuthSessionRequestDto,
    ): ApiEnvelopeDto<AuthSessionPayloadDto>

    @POST("auth/session-refreshes")
    suspend fun refreshAuthSession(
        @Body body: RefreshAuthSessionRequestDto,
    ): ApiEnvelopeDto<AuthSessionPayloadDto>

    @GET("users/me")
    suspend fun getCurrentUser(): ApiEnvelopeDto<AuthUserDto>

    @DELETE("auth/session")
    suspend fun logout(): Unit
}
