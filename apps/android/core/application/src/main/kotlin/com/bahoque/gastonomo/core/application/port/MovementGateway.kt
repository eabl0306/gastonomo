package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.time.LocalDate

/** Una pagina de movimientos. `nextCursor` es null cuando no hay mas. */
data class MovementPage(val items: List<Movement>, val nextCursor: String?)

/**
 * Lo que se manda a `POST /v1/movements`.
 *
 * Si [note] esta vacia el titulo lo pone la API con el nombre de la categoria.
 */
data class MovementDraft(
    val occurredOn: LocalDate,
    val kind: MovementKind,
    val categoryId: CategoryId,
    val money: MonetaryAmount,
    val note: String? = null,
)

/**
 * Movimientos. El limite maximo es 500 por pagina, asi que para un rango largo hay que
 * agotar el cursor antes de dibujar el balance, la tendencia o los graficos.
 *
 * El resumen de tres columnas de Movimientos se calcula sobre el periodo **sin filtrar**,
 * asi que se pide con `kind = null` y se filtra en local.
 */
interface MovementGateway {

    /** `GET /v1/movements?from=&to=&kind=&cursor=&limit=`. Las fechas son inclusive. */
    @Suppress("LongParameterList") // Es la firma del endpoint: un parametro por query param.
    suspend fun list(
        from: LocalDate,
        to: LocalDate,
        kind: MovementKind? = null,
        cursor: String? = null,
        limit: Int? = null,
    ): Outcome<MovementPage, ApiError>

    /** `POST /v1/movements` con `Idempotency-Key`. */
    suspend fun create(
        draft: MovementDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Movement>, ApiError>
}
