package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.time.LocalDate

/**
 * Movimientos programables, con paginacion de verdad.
 *
 * [pages] son las paginas que devuelve `list` en orden, para poder probar que un caso de
 * uso agota el cursor. Con una sola pagina se comporta como una lista corriente.
 */
class FakeMovementGateway(
    private val pages: List<MovementPage> = listOf(MovementPage(emptyList(), null)),
    private val failure: ApiError? = null,
    private val replay: Boolean = false,
) : MovementGateway {

    /** Cuantas veces se pidio una pagina. */
    var listCalls: Int = 0
        private set

    /** Las claves de idempotencia con las que se llamo a create, en orden. */
    val keys = mutableListOf<IdempotencyKey>()

    /** El ultimo borrador enviado. */
    var lastDraft: MovementDraft? = null
        private set

    override suspend fun list(
        from: LocalDate,
        to: LocalDate,
        kind: MovementKind?,
        cursor: String?,
        limit: Int?,
    ): Outcome<MovementPage, ApiError> {
        failure?.let { return Outcome.Failure(it) }
        val page = pages.getOrElse(listCalls) { MovementPage(emptyList(), null) }
        listCalls++
        return Outcome.Success(page)
    }

    override suspend fun create(
        draft: MovementDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Movement>, ApiError> {
        keys += key
        lastDraft = draft
        failure?.let { return Outcome.Failure(it) }
        return Outcome.Success(IdempotentResult(SampleFinance.groceries, replay))
    }
}

/** Prestamos y abonos programables. */
class FakeLoanGateway(
    private val loans: List<Loan> = SampleFinance.loans,
    private val failure: ApiError? = null,
) : LoanGateway {

    /** Las claves de idempotencia recibidas, en orden. */
    val keys = mutableListOf<IdempotencyKey>()

    /** La ultima direccion con la que se filtro la lista. */
    var lastDirection: LoanDirection? = null
        private set

    override suspend fun list(direction: LoanDirection?): Outcome<List<Loan>, ApiError> {
        lastDirection = direction
        failure?.let { return Outcome.Failure(it) }
        return Outcome.Success(loans.filter { direction == null || it.direction == direction })
    }

    override suspend fun detail(id: LoanId): Outcome<Loan, ApiError> {
        failure?.let { return Outcome.Failure(it) }
        return loans.firstOrNull { it.id == id }
            ?.let { Outcome.Success(it) }
            ?: Outcome.Failure(ApiError.NotFound)
    }

    override suspend fun create(
        draft: LoanDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Loan>, ApiError> {
        keys += key
        failure?.let { return Outcome.Failure(it) }
        return Outcome.Success(IdempotentResult(loans.first(), false))
    }

    override suspend fun addPayment(
        id: LoanId,
        draft: LoanPaymentDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<LoanPayment>, ApiError> {
        keys += key
        failure?.let { return Outcome.Failure(it) }
        return Outcome.Success(IdempotentResult(SampleFinance.payment, false))
    }
}
