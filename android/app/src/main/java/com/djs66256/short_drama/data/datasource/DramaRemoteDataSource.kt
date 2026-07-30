package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.BookingAssetsResponseDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import com.djs66256.short_drama.data.dto.ErrorDto
import com.djs66256.short_drama.data.dto.TheaterFeedResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Remote data source wrapping Retrofit [ApiService] calls.
 * Converts Retrofit responses and exceptions into [ApiResult].
 */
@Singleton
class DramaRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
    private val json: Json,
) {
    suspend fun getDramas(page: Int, pageSize: Int): ApiResult<DramaListResponseDto> = execute {
        apiService.getDramas(page, pageSize)
    }

    suspend fun getDramaDetail(id: String): ApiResult<Unit> = execute {
        apiService.getDramaDetail(id)
    }

    suspend fun getTheaterFeed(
        channel: String,
        page: Int,
        pageSize: Int,
    ): ApiResult<TheaterFeedResponseDto> = execute {
        apiService.getDramaChannel(channel, page, pageSize)
    }

    suspend fun getUserBookings(
        status: String,
        page: Int,
        pageSize: Int,
    ): ApiResult<BookingAssetsResponseDto> = execute {
        apiService.getUserBookings(status = status, page = page, pageSize = pageSize)
    }

    private suspend fun <T> execute(request: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(request())
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (httpException: HttpException) {
            parseErrorResult(httpException)
        } catch (exception: Exception) {
            ApiResult.Exception(exception)
        }
    }

    private fun parseErrorResult(httpException: HttpException): ApiResult.Error {
        val errorBody = httpException.response()?.errorBody()?.string().orEmpty()
        val parsedError = runCatching {
            json.decodeFromString(ErrorDto.serializer(), errorBody)
        }.getOrNull()

        return ApiResult.Error(
            code = parsedError?.error?.code.orEmpty().ifBlank { "HTTP_${httpException.code()}" },
            message = parsedError?.error?.message.orEmpty().ifBlank {
                httpException.message().orEmpty().ifBlank { "请求失败，请重试" }
            },
        )
    }
}
