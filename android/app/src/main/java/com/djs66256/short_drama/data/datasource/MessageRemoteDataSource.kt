package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.ErrorDto
import com.djs66256.short_drama.data.dto.InteractionMessageListResponseDto
import com.djs66256.short_drama.data.dto.MessagePreviewDto
import com.djs66256.short_drama.data.dto.SystemMessageListResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class MessageRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
    private val json: Json,
) {
    suspend fun getMessagePreview(): ApiResult<MessagePreviewDto?> {
        return try {
            val response = apiService.getMessagePreview()
            if (response.code() == NO_CONTENT_CODE) {
                ApiResult.Success(null)
            } else if (response.isSuccessful) {
                ApiResult.Success(response.body())
            } else {
                ApiResult.Error(
                    code = "HTTP_${response.code()}",
                    message = response.message().ifBlank { DEFAULT_PREVIEW_ERROR_MESSAGE },
                )
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (httpException: HttpException) {
            parseErrorResult(httpException)
        } catch (exception: Exception) {
            ApiResult.Exception(exception)
        }
    }

    suspend fun getSystemMessages(page: Int, pageSize: Int): ApiResult<SystemMessageListResponseDto> {
        return call {
            apiService.getSystemMessages(page = page, pageSize = pageSize)
        }
    }

    suspend fun getInteractionMessages(page: Int, pageSize: Int): ApiResult<InteractionMessageListResponseDto> {
        return call {
            apiService.getInteractionMessages(page = page, pageSize = pageSize)
        }
    }

    private suspend fun <T> call(block: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
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
                httpException.message().orEmpty().ifBlank { DEFAULT_ERROR_MESSAGE }
            },
        )
    }

    private companion object {
        const val NO_CONTENT_CODE = 204
        const val DEFAULT_PREVIEW_ERROR_MESSAGE = "消息预览加载失败，稍后重试"
        const val DEFAULT_ERROR_MESSAGE = "消息加载失败，请稍后重试"
    }
}
