package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.FieldError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * El cuerpo de error de la API (RFC 9457). Todos los errores lo usan, incluidos los 404 y
 * 405 de una ruta desconocida.
 *
 * `code` solo aparece en los 409 y `errors` solo en los 422.
 */
@Serializable
internal data class ProblemJson(
    @SerialName("type") val type: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: Int? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("errors") val errors: List<ProblemFieldError> = emptyList(),
)

@Serializable
internal data class ProblemFieldError(
    @SerialName("field") val field: String,
    @SerialName("message") val message: String,
)

/**
 * Traduce un `problem+json` a un error del dominio.
 *
 * Los mensajes de `errors[]` se copian tal cual: vienen en castellano y listos para pintar
 * junto al campo.
 */
internal fun ProblemJson?.toApiError(httpStatus: Int, retryAfterSeconds: Long? = null): ApiError =
    when (httpStatus) {
        STATUS_UNPROCESSABLE -> ApiError.Validation(
            fields = this?.errors?.map { FieldError(it.field, it.message) }.orEmpty(),
            detail = this?.detail,
        )
        STATUS_CONFLICT -> ApiError.Conflict(code = this?.code, detail = this?.detail)
        STATUS_UNAUTHORIZED -> unauthorizedError(this)
        STATUS_NOT_FOUND -> ApiError.NotFound
        STATUS_TOO_MANY_REQUESTS -> ApiError.TooManyRequests(retryAfterSeconds)
        STATUS_UNAVAILABLE -> ApiError.ServiceUnavailable
        else -> ApiError.Unexpected(status = httpStatus, detail = this?.detail ?: this?.title)
    }

/**
 * El 401 de login es "correo o contrasena incorrectos" y el de una peticion con sesion es
 * "hay que volver a entrar". Los distingue el `type` que manda la API.
 */
private fun unauthorizedError(problem: ProblemJson?): ApiError =
    if (problem?.type?.endsWith(INVALID_CREDENTIALS) == true) {
        ApiError.InvalidCredentials
    } else {
        ApiError.Unauthorized
    }

internal const val STATUS_UNAUTHORIZED = 401
internal const val STATUS_NOT_FOUND = 404
internal const val STATUS_CONFLICT = 409
internal const val STATUS_UNPROCESSABLE = 422
internal const val STATUS_TOO_MANY_REQUESTS = 429
internal const val STATUS_UNAVAILABLE = 503

private const val INVALID_CREDENTIALS = "invalid-credentials"
