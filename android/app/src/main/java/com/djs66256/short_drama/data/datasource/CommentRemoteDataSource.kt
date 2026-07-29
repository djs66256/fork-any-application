package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.CommentDto
import com.djs66256.short_drama.data.dto.CommentListResponseDto
import com.djs66256.short_drama.data.dto.CreateCommentRequestDto
import com.djs66256.short_drama.data.dto.ErrorDto
import com.djs66256.short_drama.data.dto.ToggleCommentLikeResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class CommentRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
    private val json: Json,
) {
    suspend fun getComments(
        dramaId: String,
        page: Int,
        pageSize: Int,
        sort: String,
    ): ApiResult<CommentListResponseDto> = execute {
        apiService.getDramaComments(
            id = dramaId,
            page = page,
            pageSize = pageSize,
            sort = sort,
        )
    }

    suspend fun createComment(
        dramaId: String,
        request: CreateCommentRequestDto,
    ): ApiResult<CommentDto> = execute {
        apiService.createDramaComment(id = dramaId, request = request)
    }

    suspend fun toggleLike(
        dramaId: String,
        commentId: String,
    ): ApiResult<ToggleCommentLikeResponseDto> = execute {
        apiService.toggleDramaCommentLike(id = dramaId, commentId = commentId)
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
