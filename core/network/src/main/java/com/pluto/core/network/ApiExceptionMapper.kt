package com.pluto.core.network

import com.pluto.core.common.ApiException

/**
 * Extension: convert HTTP exceptions to typed [ApiException]s.
 *
 * Lives in core/network (not core/common) to avoid a circular module
 * dependency. The data layer calls this when wrapping repository
 * results into [com.pluto.core.common.Result].
 */
fun ApiException.Companion.fromHttp(httpException: Throwable): ApiException = when (httpException) {
    is ApiException -> httpException
    is HttpClientException -> when (httpException.code) {
        401 -> ApiException.Unauthorized(httpException)
        404 -> ApiException.NotFound(httpException)
        else -> ApiException.ServerError(httpException.code, httpException)
    }
    is HttpServerException -> ApiException.ServerError(httpException.code, httpException)
    else -> fromException(httpException)
}
