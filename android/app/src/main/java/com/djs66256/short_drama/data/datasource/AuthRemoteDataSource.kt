package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.ApiEnvelopeDto
import com.djs66256.short_drama.data.dto.AuthSessionPayloadDto
import com.djs66256.short_drama.data.dto.AuthUserDto
import com.djs66256.short_drama.data.dto.CreateAuthSessionRequestDto
import com.djs66256.short_drama.data.dto.ErrorDto
import com.djs66256.short_drama.data.dto.RefreshAuthSessionRequestDto
import com.djs66256.short_drama.data.dto.SendOtpPayloadDto
import com.djs66256.short_drama.data.dto.SendOtpRequestDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val apiService: ApiService,
    private val json: Json,
) {
    suspend fun sendOtp(
        countryCode: String,
        phone: String,
        scene: String,
    ): ApiResult<ApiEnvelopeDto<SendOtpPayloadDto>> = execute {
        apiService.sendOtpRequest(
            SendOtpRequestDto(
                countryCode = countryCode,
                phone = phone,
                scene = scene,
            ),
        )
    }

    suspend fun createSession(
        countryCode: String,
        phone: String,
        code: String,
    ): ApiResult<ApiEnvelopeDto<AuthSessionPayloadDto>> = execute {
        apiService.createAuthSession(
            CreateAuthSessionRequestDto(
                countryCode = countryCode,
                phone = phone,
                code = code,
            ),
        )
    }

    suspend fun refreshSession(
        refreshToken: String,
    ): ApiResult<ApiEnvelopeDto<AuthSessionPayloadDto>> = execute {
        apiService.refreshAuthSession(
            RefreshAuthSessionRequestDto(refreshToken = refreshToken),
        )
    }

    suspend fun getCurrentUser(): ApiResult<ApiEnvelopeDto<AuthUserDto>> = execute {
        apiService.getCurrentUser()
    }

    suspend fun logout(): ApiResult<Unit> = execute {
        apiService.logout()
        Unit
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
