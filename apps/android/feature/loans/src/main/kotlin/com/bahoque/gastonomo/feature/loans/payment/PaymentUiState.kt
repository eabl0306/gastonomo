package com.bahoque.gastonomo.feature.loans.payment

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.feature.loans.LoanSaveFailure
import com.bahoque.gastonomo.feature.loans.MoneyContext
import com.bahoque.gastonomo.feature.loans.MoneyEntryState
import java.time.LocalDate

/**
 * El sheet de Registrar abono, que se abre encima de Prestamos y del detalle sin navegar.
 *
 * @property loan null cuando el sheet esta cerrado. Lo trae quien lo abre, asi que el sheet
 *   no vuelve a pedir el prestamo a la API.
 * @property outstanding el saldo con el que se rellenan los chips "Todo el saldo" y
 *   "La mitad", ya en la moneda principal.
 * @property idempotencyKey la clave de este intento de guardado. Se conserva entre
 *   reintentos y solo se tira cuando la creacion sale bien o cambia el cuerpo.
 */
@Immutable
data class PaymentUiState(
    val loan: Loan? = null,
    val outstanding: Money = Money.zero(CurrencyCode.COP),
    val entry: MoneyEntryState = MoneyEntryState(),
    val date: LocalDate = FALLBACK_DATE,
    val context: MoneyContext = MoneyContext(),
    val saving: Boolean = false,
    val failure: LoanSaveFailure? = null,
    val idempotencyKey: IdempotencyKey? = null,
) {
    /** True cuando hay que pintar el sheet. */
    val isOpen: Boolean get() = loan != null

    /** El boton "Guardar abono" se habilita con importe y equivalencia mayores que cero. */
    val canSave: Boolean get() = !saving && entry.suggestion(context).isValid
}

/** Lo mismo que en el formulario: `LocalDate.EPOCH` pide API 34 y el minimo es 29. */
private val FALLBACK_DATE: LocalDate = LocalDate.of(1970, 1, 1)
