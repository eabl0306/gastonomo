package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import java.time.LocalDate

/** Lo que se manda a `POST /v1/loans`. Crea tambien su movimiento, en una sola escritura. */
data class LoanDraft(
    val person: String,
    val direction: LoanDirection,
    val openedOn: LocalDate,
    val money: MonetaryAmount,
    val note: String? = null,
)

/** Lo que se manda a `POST /v1/loans/{id}/payments`. Crea tambien su movimiento. */
data class LoanPaymentDraft(
    val paidOn: LocalDate,
    val money: MonetaryAmount,
    val label: String? = null,
)

/**
 * Prestamos, con sus abonos embebidos. El estado, el porcentaje, los intereses ganados y
 * el orden de la lista los deriva el cliente: dependen de la moneda principal del momento
 * y la API no los guarda.
 */
interface LoanGateway {

    /** `GET /v1/loans?direction=`. */
    suspend fun list(direction: LoanDirection? = null): Outcome<List<Loan>, ApiError>

    /** `GET /v1/loans/{id}`. Un prestamo de otra cuenta responde 404. */
    suspend fun detail(id: LoanId): Outcome<Loan, ApiError>

    /** `POST /v1/loans` con `Idempotency-Key`. */
    suspend fun create(
        draft: LoanDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Loan>, ApiError>

    /** `POST /v1/loans/{id}/payments` con `Idempotency-Key`. Un abono puede pasarse del saldo. */
    suspend fun addPayment(
        id: LoanId,
        draft: LoanPaymentDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<LoanPayment>, ApiError>
}
