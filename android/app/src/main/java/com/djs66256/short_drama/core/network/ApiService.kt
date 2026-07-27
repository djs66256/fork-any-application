package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.HotSearchListResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
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

    @POST("dramas")
    suspend fun createDrama(@Body body: Map<String, String>): Unit

    @GET("dramas/{id}")
    suspend fun getDramaDetail(@Path("id") id: String): Unit

    @GET("episodes/{id}")
    suspend fun getEpisodeDetail(@Path("id") id: String): Unit

    @POST("player/start")
    suspend fun startPlayer(@Body body: Map<String, String>): Unit

    @POST("player/stop")
    suspend fun stopPlayer(@Body body: Map<String, String>): Unit
}
