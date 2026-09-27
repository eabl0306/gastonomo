package com.bahoque.gastonomo.core.domain.loan

import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import java.time.LocalDate

/**
 * Un prestamo con sus abonos embebidos. Vienen dentro porque la tarjeta ya necesita el
 * saldo, y el saldo es el prestamo menos la suma de los abonos con las tasas de hoy.
 */
data class Loan(
    val id: LoanId,
    val person: String,
    val note: String,
    val direction: LoanDirection,
    val openedOn: LocalDate,
    val money: MonetaryAmount,
    val payments: List<LoanPayment> = emptyList(),
    /** True mientras el prestamo solo existe en este telefono. Ver [Movement.pending]. */
    val pending: Boolean = false,
)

/** Identificador de un prestamo. Es un UUID en la API. */
@JvmInline
value class LoanId(val value: String) {
    override fun toString(): String = value
}
