package com.bahoque.gastonomo.core.domain.movement

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import java.time.LocalDate

/**
 * Un ingreso o un gasto. La fecha es un dia sin hora ni zona: un gasto del 22 de
 * septiembre es del 22 en Bogota y en Madrid.
 *
 * @property title la nota del usuario o, si no la escribio, el nombre de la categoria.
 * @property loanId lo rellena la API cuando el movimiento lo genero un prestamo.
 * @property loanPaymentId lo rellena la API cuando lo genero un abono.
 * @property pending true mientras el movimiento solo existe en este telefono, porque se
 *   registro sin conexion y todavia no se ha subido. Cuenta para el balance: es dinero que el
 *   usuario ya gasto.
 */
data class Movement(
    val id: MovementId,
    val occurredOn: LocalDate,
    val kind: MovementKind,
    val title: String,
    val money: MonetaryAmount,
    val category: Category,
    val loanId: String? = null,
    val loanPaymentId: String? = null,
    val pending: Boolean = false,
)

/** Identificador de un movimiento. Es un UUID en la API. */
@JvmInline
value class MovementId(val value: String) {
    override fun toString(): String = value
}
