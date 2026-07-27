package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.ErrorDto
import com.djs66256.short_drama.data.dto.HotSearchListResponseDto
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class SearchRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun searchDramas(
        query: String,
        page: Int,
        pageSize: Int,
    ): ApiResult<DramaListResponseDto> = execute {
        apiService.searchDramas(query = query, page = page, pageSize = pageSize)
    }

    suspend fun getHotSearches(): ApiResult<HotSearchListResponseDto> = execute {
        apiService.getHotSearches()
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
