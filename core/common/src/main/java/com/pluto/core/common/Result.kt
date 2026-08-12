package com.pluto.core.common

/**
 * Result — generic sealed result type for the data layer.
 *
 * Mirrors CCloud's `util/Result.kt` shape exactly so the existing
 * repository pattern translates 1:1. Every repository returns Result<T>.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: ApiException) : Result<Nothing>()
    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): ApiException? = (this as? Error)?.exception

    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (ApiException) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> error(exception: ApiException): Result<T> = Error(exception)
        fun <T> loading(): Result<T> = Loading
    }
}
