package com.bahoque.gastonomo.feature.dictation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.SpeechFailure

/**
 * Lo que puede salir mal al interpretar un dictado, ya reducido a lo que el usuario puede
 * hacer al respecto.
 *
 * `NOT_APPROVED` sale de un 404, que es lo que responde la API a una cuenta sin el dictado
 * aprobado: igual que cualquier recurso que no es suyo, porque un 403 confirmaria que existe.
 */
enum class DictationProblem {
    NOT_APPROVED,
    NETWORK,
    SERVICE,
    TOO_MANY,
    UNCLEAR,
    UNEXPECTED,
    ;

    companion object {

        /**
         * El error de interpretar. Un 404 es la cuenta sin aprobar y un 422 es un texto que el
         * interprete no supo convertir en un registro.
         */
        fun ofParse(error: ApiError): DictationProblem = when (error) {
            ApiError.NotFound -> NOT_APPROVED
            ApiError.Network -> NETWORK
            ApiError.ServiceUnavailable -> SERVICE
            is ApiError.TooManyRequests -> TOO_MANY
            is ApiError.Validation -> UNCLEAR
            else -> UNEXPECTED
        }

        /**
         * El error de guardar, que son otros. Aqui un 404 o un 422 no significan nada que el
         * usuario pueda arreglar desde la tarjeta: el borrador lo compuso la API hace un segundo.
         * Sin conexion no llega a fallar, porque el registro se encola.
         */
        fun ofSave(error: ApiError): DictationProblem = when (error) {
            ApiError.Network -> NETWORK
            ApiError.ServiceUnavailable -> SERVICE
            is ApiError.TooManyRequests -> TOO_MANY
            else -> UNEXPECTED
        }
    }
}

/** El mensaje del problema, en el idioma de la cuenta. */
@Composable
internal fun messageOf(problem: DictationProblem): String = stringResource(
    when (problem) {
        DictationProblem.NOT_APPROVED -> R.string.dictation_error_unavailable
        DictationProblem.NETWORK -> R.string.dictation_error_network
        DictationProblem.SERVICE -> R.string.dictation_error_service
        DictationProblem.TOO_MANY -> R.string.dictation_error_too_many
        DictationProblem.UNCLEAR -> R.string.dictation_error_unclear
        DictationProblem.UNEXPECTED -> R.string.dictation_error_unexpected
    },
)

/**
 * El mensaje de un fallo del microfono.
 *
 * Los cinco acaban en el modo escrito: el dictado no puede dejar sin salida a quien no tiene
 * microfono, no dio permiso o esta sin conexion.
 */
@Composable
internal fun messageOf(failure: SpeechFailure): String = stringResource(
    when (failure) {
        SpeechFailure.UNAVAILABLE -> R.string.dictation_mic_unavailable
        SpeechFailure.PERMISSION -> R.string.dictation_mic_denied
        SpeechFailure.OFFLINE -> R.string.dictation_mic_offline
        SpeechFailure.NO_MATCH -> R.string.dictation_mic_no_match
        SpeechFailure.FAILED -> R.string.dictation_mic_failed
    },
)
