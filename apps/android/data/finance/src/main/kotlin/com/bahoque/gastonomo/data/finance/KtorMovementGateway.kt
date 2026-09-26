package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.data.network.ApiClient
import io.ktor.client.request.parameter
import java.time.LocalDate

/** Movimientos contra la API. */
internal class KtorMovementGateway(private val client: ApiClient) : MovementGateway {

    override suspend fun list(
        from: LocalDate,
        to: LocalDate,
        kind: MovementKind?,
        cursor: String?,
        limit: Int?,
    ): Outcome<MovementPage, ApiError> = client.get<MovementPageDto>("movements") {
        parameter("from", from.toString())
        parameter("to", to.toString())
        kind?.let { parameter("kind", it.wire) }
        cursor?.let { parameter("cursor", it) }
        limit?.let { parameter("limit", it) }
    }.map { page ->
        MovementPage(items = page.items.map { it.toDomain() }, nextCursor = page.nextCursor)
    }

    override suspend fun create(
        draft: MovementDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Movement>, ApiError> {
        val money = MoneyFields.of(draft.money)
        val body = CreateMovementDto(
            occurredOn = draft.occurredOn.toString(),
            kind = draft.kind.wire,
            categoryId = draft.categoryId.value,
            amount = money.amount,
            currency = money.currency,
            base = money.base,
            baseCurrency = money.baseCurrency,
            note = draft.note?.takeIf { it.isNotBlank() },
        )
        return client.postIdempotent<MovementDto>("movements", body, key)
            .map { IdempotentResult(it.value.toDomain(), it.isReplay) }
    }
}

/** La fabrica que llama el contenedor de la aplicacion. */
fun createMovementGateway(client: ApiClient): MovementGateway = KtorMovementGateway(client)
