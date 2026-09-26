package com.bahoque.gastonomo.feature.loans

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings

/**
 * Lo que sale mal al crear un prestamo o registrar un abono, repartido por donde se pinta.
 *
 * Los mensajes de un 422 **ya vienen en castellano** desde la API y se pintan tal cual
 * junto a su campo: no se traducen ni se reescriben. Todo lo demas es un problema general
 * y ese si lo redacta el cliente.
 */
@Immutable
data class LoanSaveFailure(
    val amount: String? = null,
    val person: String? = null,
    val date: String? = null,
    val detail: String? = null,
    val problem: LoanProblem? = null,
) {
    companion object {

        /** Los nombres de campo que manda la API en `errors[]` para el importe. */
        private val AMOUNT_FIELDS = listOf("amount", "base", "currency", "baseCurrency")
        private val DATE_FIELDS = listOf("openedOn", "paidOn", "occurredOn")
        private const val PERSON_FIELD = "person"

        /** Traduce un [ApiError] a lo que las pantallas de esta tanda saben ensenar. */
        fun of(error: ApiError): LoanSaveFailure = when (error) {
            is ApiError.Validation -> ofValidation(error)
            is ApiError.Conflict -> LoanSaveFailure(problem = problemOf(error.code))
            ApiError.Network -> LoanSaveFailure(problem = LoanProblem.NETWORK)
            ApiError.Unauthorized -> LoanSaveFailure(problem = LoanProblem.SESSION_EXPIRED)
            ApiError.NotFound -> LoanSaveFailure(problem = LoanProblem.NOT_FOUND)
            ApiError.ServiceUnavailable -> LoanSaveFailure(problem = LoanProblem.RATES_UNAVAILABLE)
            else -> LoanSaveFailure(problem = LoanProblem.UNEXPECTED)
        }

        private fun ofValidation(error: ApiError.Validation): LoanSaveFailure {
            val placed = AMOUNT_FIELDS + DATE_FIELDS + PERSON_FIELD
            return LoanSaveFailure(
                amount = AMOUNT_FIELDS.firstNotNullOfOrNull(error::messageFor),
                person = error.messageFor(PERSON_FIELD),
                date = DATE_FIELDS.firstNotNullOfOrNull(error::messageFor),
                detail = error.fields.firstOrNull { it.field !in placed }?.message ?: error.detail,
            )
        }

        private fun problemOf(code: String?): LoanProblem = when (code) {
            ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS -> LoanProblem.IN_PROGRESS
            ConflictCode.IDEMPOTENCY_KEY_REUSED -> LoanProblem.BODY_CHANGED
            else -> LoanProblem.UNEXPECTED
        }
    }
}

/** Lo que puede ir mal en estas pantallas y cuyo texto escribe el cliente, no la API. */
enum class LoanProblem {
    /** 409 `idempotency_key_in_progress`: el mismo intento esta en vuelo. Esperar y repetir. */
    IN_PROGRESS,

    /** 409 `idempotency_key_reused`: cambio el cuerpo, asi que hace falta una clave nueva. */
    BODY_CHANGED,

    NETWORK,
    SESSION_EXPIRED,

    /** 404. Tambien es lo que responde un prestamo de otra cuenta: nunca un 403. */
    NOT_FOUND,

    /** 503 mientras la tarea de tasas no consigue su primera pasada. */
    RATES_UNAVAILABLE,

    UNEXPECTED,
}

/**
 * True si hay que tirar la clave de idempotencia. Solo la quema un 409 de cuerpo cambiado:
 * un 422 no, porque el usuario corrige el importe y reintenta el mismo intento.
 */
fun LoanSaveFailure.burnsIdempotencyKey(): Boolean = problem == LoanProblem.BODY_CHANGED

/**
 * El problema de una lectura. Una lectura no devuelve 422 ni conflictos de idempotencia, asi
 * que el caso general basta.
 */
fun ApiError.toLoanProblem(): LoanProblem =
    LoanSaveFailure.of(this).problem ?: LoanProblem.UNEXPECTED

/** El texto de un problema general. */
@Composable
fun messageOf(problem: LoanProblem): String = stringResource(
    when (problem) {
        LoanProblem.IN_PROGRESS -> GastonomoStrings.errorInProgress
        LoanProblem.BODY_CHANGED -> GastonomoStrings.errorBodyChanged
        LoanProblem.NETWORK -> GastonomoStrings.errorNetwork
        LoanProblem.SESSION_EXPIRED -> GastonomoStrings.errorSessionExpired
        LoanProblem.NOT_FOUND -> R.string.loan_detail_not_found
        LoanProblem.RATES_UNAVAILABLE -> GastonomoStrings.errorRatesUnavailable
        LoanProblem.UNEXPECTED -> GastonomoStrings.errorUnexpected
    },
)
