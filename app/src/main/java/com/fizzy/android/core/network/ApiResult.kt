package com.fizzy.android.core.network

import retrofit2.Response

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class Exception(val throwable: Throwable) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isException: Boolean get() = this is Exception

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw RuntimeException("API Error: $code - $message")
        is Exception -> throw throwable
    }

    fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Exception -> this
    }

    suspend fun <R> mapSuspend(transform: suspend (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Exception -> this
    }

    companion object {
        suspend fun <T> from(block: suspend () -> Response<T>): ApiResult<T> {
            return try {
                val response = block()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Success(body)
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        Success(Unit as T)
                    }
                } else {
                    Error(response.code(), response.message())
                }
            } catch (e: kotlin.Exception) {
                Exception(e)
            }
        }
    }
}

inline fun <T, R> ApiResult<T>.fold(
    onSuccess: (T) -> R,
    onError: (Int, String) -> R,
    onException: (Throwable) -> R
): R = when (this) {
    is ApiResult.Success -> onSuccess(data)
    is ApiResult.Error -> onError(code, message)
    is ApiResult.Exception -> onException(throwable)
}

inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> ApiResult<T>.onError(action: (Int, String) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) {
        action(code, message)
    }
    return this
}

inline fun <T> ApiResult<T>.onException(action: (Throwable) -> Unit): ApiResult<T> {
    if (this is ApiResult.Exception) {
        action(throwable)
    }
    return this
}
