package com.djs66256.short_drama.core.network

import com.djs66256.short_drama.core.auth.AuthStateHolder
import com.djs66256.short_drama.core.di.AuthIoDispatcher
import com.djs66256.short_drama.core.di.RefreshApiService
import com.djs66256.short_drama.data.dto.ErrorDto
import com.djs66256.short_drama.data.dto.RefreshAuthSessionRequestDto
import com.djs66256.short_drama.data.dto.toDomain
import com.djs66256.short_drama.domain.model.AuthSession
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class AuthRefreshCoordinator @Inject constructor(
    @RefreshApiService private val refreshApiService: ApiService,
    private val authStateHolder: AuthStateHolder,
    private val json: Json,
    @AuthIoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val lock = Any()
    @Volatile
    private var inFlightRefresh: CompletableFuture<ApiResult<AuthSession>>? = null

    fun refreshBlocking(): ApiResult<AuthSession> {
        val refreshToken = authStateHolder.refreshToken()
            ?: return ApiResult.Error(
                code = NO_REFRESH_TOKEN_CODE,
                message = NO_REFRESH_TOKEN_MESSAGE,
            )

        var shouldExecute = false
        val future = synchronized(lock) {
            inFlightRefresh ?: CompletableFuture<ApiResult<AuthSession>>().also {
                inFlightRefresh = it
                shouldExecute = true
            }
        }

        if (shouldExecute) {
            val result = runBlocking(ioDispatcher) {
                performRefresh(refreshToken)
            }
            future.complete(result)
            synchronized(lock) {
                if (inFlightRefresh === future) {
                    inFlightRefresh = null
                }
            }
        }

        return future.get()
    }

    private suspend fun performRefresh(refreshToken: String): ApiResult<AuthSession> {
        authStateHolder.markRefreshing()
        return try {
            val response = refreshApiService.refreshAuthSession(
                RefreshAuthSessionRequestDto(refreshToken = refreshToken),
            )
            val session = response.data.toDomain()
            authStateHolder.updateSession(session)
            ApiResult.Success(session)
        } catch (httpException: HttpException) {
            authStateHolder.markExpired()
            authStateHolder.clearSession()
            parseErrorResult(httpException)
        } catch (throwable: Throwable) {
            authStateHolder.markExpired()
            authStateHolder.clearSession()
            ApiResult.Exception(throwable)
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
                httpException.message().orEmpty().ifBlank { NO_REFRESH_TOKEN_MESSAGE }
            },
        )
    }

    companion object {
        const val NO_REFRESH_TOKEN_CODE = "AUTH_NO_REFRESH_TOKEN"
        const val NO_REFRESH_TOKEN_MESSAGE = "登录态已失效，请重新登录"
    }
}
