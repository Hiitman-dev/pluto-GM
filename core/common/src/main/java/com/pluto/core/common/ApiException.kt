package com.pluto.core.common

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * ApiException — typed exceptions for the API / data layer.
 *
 * Mirrors CCloud's `util/ApiException.kt` 1:1. The UI layer never sees
 * raw exceptions — it always sees one of these typed variants, so error
 * states can be rendered with the cosmic "SIGNAL LOST" language.
 */
sealed class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class NetworkError(val cause_: Throwable? = null) :
        ApiException("Connection lost. Please check your network.", cause_)

    data class ServerError(val code: Int, val cause_: Throwable? = null) :
        ApiException("Signal interrupted. Please try again later.", cause_)

    data class ParseError(val cause_: Throwable? = null) :
        ApiException("Failed to interpret the signal from the network.", cause_)

    data class NotFound(val cause_: Throwable? = null) :
        ApiException("Nothing found in this sector.", cause_)

    data class Unauthorized(val cause_: Throwable? = null) :
        ApiException("Authentication required.", cause_)

    data class SignalLost(val cause_: Throwable? = null) :
        ApiException("Unable to reach the PLUTO network.", cause_)

    data class UnknownError(val cause_: Throwable? = null) :
        ApiException("An unexpected anomaly occurred.", cause_)

    companion object {
        fun fromException(e: Throwable): ApiException = when (e) {
            is ApiException -> e
            is UnknownHostException -> NetworkError(e)
            is SocketTimeoutException -> NetworkError(e)
            is IOException -> NetworkError(e)
            else -> UnknownError(e)
        }
    }
}
