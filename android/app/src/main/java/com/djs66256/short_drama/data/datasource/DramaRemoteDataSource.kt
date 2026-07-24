package com.djs66256.short_drama.data.datasource

import com.djs66256.short_drama.core.network.ApiResult
import com.djs66256.short_drama.core.network.ApiService
import com.djs66256.short_drama.data.dto.DramaListResponseDto
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source wrapping Retrofit [ApiService] calls.
 * Converts Retrofit responses and exceptions into [ApiResult].
 */
@Singleton
class DramaRemoteDataSource @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getDramas(page: Int, pageSize: Int): ApiResult<DramaListResponseDto> {
        return try {
            val response = apiService.getDramas(page, pageSize)
            ApiResult.Success(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }

    suspend fun getDramaDetail(id: String): ApiResult<Unit> {
        return try {
            apiService.getDramaDetail(id)
            ApiResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Exception(e)
        }
    }
}
