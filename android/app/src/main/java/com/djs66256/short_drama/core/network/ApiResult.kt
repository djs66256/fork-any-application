package com.djs66256.short_drama.core.network

/**
 * Sealed class representing the three possible outcomes of a network call.
 *
 * - [Success]: The call completed successfully with data of type [T].
 * - [Error]: The server returned an error response with a known structure.
 * - [Exception]: An unexpected error occurred (network, serialization, etc.).
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()
    data class Exception(val throwable: Throwable) : ApiResult<Nothing>()
}
