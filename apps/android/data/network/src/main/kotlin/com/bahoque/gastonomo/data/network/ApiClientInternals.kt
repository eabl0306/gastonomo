package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/** La URL completa de una ruta de negocio, con el prefijo de version. */
internal fun ApiConfig.urlOf(path: String): String =
    "${baseUrl.trimEnd('/')}/${ApiConfig.VERSION_PATH}/${path.trimStart('/')}"

/** Cuerpo JSON de una peticion. */
internal fun HttpRequestBuilder.setJsonBody(body: Any) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

/** True si la API marco la respuesta como reintento de algo ya creado. */
@PublishedApi
internal fun HttpResponse.isReplay(): Boolean =
    headers[ApiClient.IDEMPOTENT_REPLAY]?.equals("true", ignoreCase = true) == true

/** Convierte una respuesta en un [Outcome], leyendo el `problem+json` si hubo error. */
@PublishedApi
internal suspend fun HttpResponse.toOutcome(): Outcome<HttpResponse, ApiError> {
    if (status.isSuccess()) return Outcome.Success(this)
    val problem = runCatching { body<ProblemJson>() }.getOrNull()
    return Outcome.Failure(problem.toApiError(status.value, retryAfterSeconds()))
}

private fun HttpResponse.retryAfterSeconds(): Long? =
    headers[HttpHeaders.RetryAfter]?.toLongOrNull()

/** Lee el cuerpo de una respuesta ya validada. */
@PublishedApi
internal suspend inline fun <reified T> Outcome<HttpResponse, ApiError>.decode(): Outcome<T, ApiError> =
    when (this) {
        is Outcome.Failure -> this
        is Outcome.Success -> runCatching { Outcome.Success(value.body<T>()) }
            .getOrElse { Outcome.Failure(ApiError.Unexpected(detail = it.message)) }
    }
