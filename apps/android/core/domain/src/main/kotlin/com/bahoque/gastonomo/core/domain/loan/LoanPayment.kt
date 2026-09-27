package com.bahoque.gastonomo.core.domain.loan

import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import java.time.LocalDate

/**
 * Un abono de un prestamo.
 *
 * @property label el concepto opcional, que la pantalla ensena como "Abono - {concepto}".
 */
data class LoanPayment(
    val id: LoanPaymentId,
    val loanId: LoanId,
    val paidOn: LocalDate,
    val label: String?,
    val money: MonetaryAmount,
    /** True mientras el abono solo existe en este telefono. */
    val pending: Boolean = false,
)

/** Identificador de un abono. Es un UUID en la API. */
@JvmInline
value class LoanPaymentId(val value: String) {
    override fun toString(): String = value
}
