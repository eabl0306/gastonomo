package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.data.network.ApiClient
import io.ktor.client.request.parameter

/** Prestamos y abonos contra la API. Los abonos viajan dentro del prestamo. */
internal class KtorLoanGateway(private val client: ApiClient) : LoanGateway {

    override suspend fun list(direction: LoanDirection?): Outcome<List<Loan>, ApiError> =
        client.get<LoanListDto>("loans") {
            direction?.let { parameter("direction", it.wire) }
        }.map { list -> list.items.map { it.toDomain() } }

    override suspend fun detail(id: LoanId): Outcome<Loan, ApiError> =
        client.get<LoanDto>("loans/${id.value}").map { it.toDomain() }

    override suspend fun create(
        draft: LoanDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Loan>, ApiError> {
        val money = MoneyFields.of(draft.money)
        val body = CreateLoanDto(
            person = draft.person,
            direction = draft.direction.wire,
            openedOn = draft.openedOn.toString(),
            amount = money.amount,
            currency = money.currency,
            base = money.base,
            baseCurrency = money.baseCurrency,
            note = draft.note?.takeIf { it.isNotBlank() },
        )
        return client.postIdempotent<LoanDto>("loans", body, key)
            .map { IdempotentResult(it.value.toDomain(), it.isReplay) }
    }

    override suspend fun addPayment(
        id: LoanId,
        draft: LoanPaymentDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<LoanPayment>, ApiError> {
        val money = MoneyFields.of(draft.money)
        val body = CreateLoanPaymentDto(
            paidOn = draft.paidOn.toString(),
            amount = money.amount,
            currency = money.currency,
            base = money.base,
            baseCurrency = money.baseCurrency,
            label = draft.label?.takeIf { it.isNotBlank() },
        )
        return client.postIdempotent<LoanPaymentDto>("loans/${id.value}/payments", body, key)
            .map { IdempotentResult(it.value.toDomain(), it.isReplay) }
    }
}

/** La fabrica que llama el contenedor de la aplicacion. */
fun createLoanGateway(client: ApiClient): LoanGateway = KtorLoanGateway(client)
