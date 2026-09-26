package com.bahoque.gastonomo.feature.loans.form

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.LoanSaveFailure
import com.bahoque.gastonomo.feature.loans.MoneyContext
import com.bahoque.gastonomo.feature.loans.MoneyEntryState
import java.time.LocalDate

/**
 * Pantalla 10. El formulario de Nuevo prestamo.
 *
 * La API crea el prestamo y su movimiento en la misma escritura, asi que esto produce un
 * solo POST y hace falta la clave de idempotencia.
 *
 * @property idempotencyKey la clave de este intento. Se conserva entre reintentos y solo se
 *   tira cuando el prestamo se crea o cuando la API avisa de que el cuerpo cambio.
 */
@Immutable
data class NewLoanUiState(
    val direction: LoanDirection = LoanDirection.LENT,
    val person: String = "",
    val note: String = "",
    val date: LocalDate = FALLBACK_DATE,
    val entry: MoneyEntryState = MoneyEntryState(),
    val context: MoneyContext = MoneyContext(),
    val saving: Boolean = false,
    val failure: LoanSaveFailure? = null,
    val idempotencyKey: IdempotencyKey? = null,
) {
    /** "Crear prestamo" pide importe, equivalencia y persona. */
    val canSave: Boolean
        get() = !saving && person.isNotBlank() && entry.suggestion(context).isValid
}

/**
 * La fecha del estado inicial, que se sustituye por la del reloj en cuanto el ViewModel
 * arranca. No es `LocalDate.EPOCH`: ese campo pide API 34 y el minimo de la aplicacion es 29.
 */
private val FALLBACK_DATE: LocalDate = LocalDate.of(1970, 1, 1)
