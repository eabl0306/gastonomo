package com.bahoque.gastonomo.data.database.local

import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.domain.loan.LoanId

/**
 * Una escritura que el usuario hizo sin conexion y que todavia no ha llegado a la API.
 *
 * @property id el mismo identificador con el que se guardo el registro provisional, para poder
 *   sustituirlo por el de verdad cuando la API conteste.
 * @property idempotencyKey se genera al encolar y **no se regenera nunca**. Ver el ADR 0012.
 * @property dependsOn el [id] de otra escritura que tiene que subir antes. Un abono de un prestamo
 *   que todavia no existe en el servidor no se puede subir.
 * @property failure el motivo del ultimo fallo permanente. Mientras es null, se reintenta.
 */
data class PendingWrite(
    val id: String,
    val idempotencyKey: IdempotencyKey,
    val draft: PendingDraft,
    val loanId: LoanId? = null,
    val dependsOn: String? = null,
    val queuedAt: Long = 0,
    val attempts: Int = 0,
    val failure: String? = null,
) {
    /** True cuando ya no se reintenta sola y hace falta que el usuario la corrija. */
    val needsFixing: Boolean get() = failure != null
}

/** Lo que hay que enviar, segun que registro sea. */
sealed interface PendingDraft {

    data class Movement(val value: MovementDraft) : PendingDraft

    data class Loan(val value: LoanDraft) : PendingDraft

    data class Payment(val value: LoanPaymentDraft) : PendingDraft
}
