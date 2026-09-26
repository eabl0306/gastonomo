package com.bahoque.gastonomo.feature.auth

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings

/**
 * Un mensaje listo para pintar, sin que el ViewModel toque recursos de Android.
 *
 * Hay dos clases de mensaje y por eso hay dos variantes: los nuestros, que se traducen, y
 * los que manda la API en `errors[].message` o en `detail`, que **ya vienen en castellano**
 * y no se traducen.
 */
sealed interface AuthMessage {

    /** Un texto propio, de `strings.xml`. */
    data class Resource(@get:StringRes val id: Int) : AuthMessage

    /**
     * Un texto propio que cambia con una cuenta, como "Te quedan 3 intentos". El numero se
     * pasa tambien como argumento del recurso.
     */
    data class Plural(@get:PluralsRes val id: Int, val count: Int) : AuthMessage

    /** El texto tal como lo mando la API. */
    data class Literal(val text: String) : AuthMessage
}

/** Resuelve el mensaje contra los recursos del idioma vigente. */
@Composable
fun AuthMessage.text(): String = when (this) {
    is AuthMessage.Resource -> stringResource(id)
    is AuthMessage.Plural -> pluralStringResource(id, count, count)
    is AuthMessage.Literal -> text
}

/**
 * El mensaje de formulario de un fallo de la API. Los 422 no pasan por aqui: sus errores
 * son de campo y los reparte cada ViewModel con [ApiError.Validation.messageFor].
 */
fun ApiError.toAuthMessage(): AuthMessage = when (this) {
    is ApiError.Validation -> detail.orResource(GastonomoStrings.errorUnexpected)
    is ApiError.Conflict -> detail.orResource(R.string.auth_error_conflict)
    ApiError.InvalidCredentials -> AuthMessage.Resource(R.string.auth_error_invalid_credentials)
    ApiError.Unauthorized -> AuthMessage.Resource(GastonomoStrings.errorSessionExpired)
    ApiError.NotFound -> AuthMessage.Resource(GastonomoStrings.errorUnexpected)
    is ApiError.TooManyRequests -> AuthMessage.Resource(R.string.auth_error_too_many_requests)
    ApiError.ServiceUnavailable -> AuthMessage.Resource(GastonomoStrings.errorUnexpected)
    ApiError.Network -> AuthMessage.Resource(GastonomoStrings.errorNetwork)
    is ApiError.Unexpected -> detail.orResource(GastonomoStrings.errorUnexpected)
}

private fun String?.orResource(@StringRes fallback: Int): AuthMessage =
    if (isNullOrBlank()) AuthMessage.Resource(fallback) else AuthMessage.Literal(this)
