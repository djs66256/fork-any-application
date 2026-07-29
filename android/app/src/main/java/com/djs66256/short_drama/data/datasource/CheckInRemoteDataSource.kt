package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.ErrorDto
import com.djs66256.short_drama.data.dto.SignInStatusDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class CheckInRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
    private val json: Json,
) {
    suspend fun getCheckInStatus(installationId: String?): ApiResult<SignInStatusDto> {
        return call {
            apiService.getCheckInStatus(installationId = installationId)
        }
    }

    suspend fun submitCheckIn(installationId: String?): ApiResult<SignInStatusDto> {
        return call {
            apiService.submitCheckIn(installationId = installationId)
        }
    }

    private suspend fun call(block: suspend () -> SignInStatusDto): ApiResult<SignInStatusDto> {
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
        const val DEFAULT_ERROR_MESSAGE = "签到失败，请重试"
    }
}
