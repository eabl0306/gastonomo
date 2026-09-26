package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.state.MessageKey
import com.bahoque.gastonomo.core.application.state.PeriodSelection
import com.bahoque.gastonomo.core.application.state.RangeDraft
import com.bahoque.gastonomo.core.domain.analysis.DayGroup
import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.time.LocalDate

/**
 * Todo lo que pinta la lista de Movimientos.
 *
 * El resumen de tres columnas se calcula sobre el periodo **sin filtrar** y las pills solo
 * filtran la lista de abajo: por eso hay dos cosas en el estado, el resumen y los grupos, y no
 * una sola. Si se pidiera la lista con `kind=out` no se podria pintar la columna de Ingresos.
 */
data class MovementsUiState(
    val today: LocalDate,
    val selection: PeriodSelection,
    val displayCurrency: CurrencyCode = CurrencyCode.COP,
    val rates: ExchangeRates = ExchangeRates.EMPTY,
    val filter: MovementFilter = MovementFilter.ALL,
    val summary: PeriodSummary = zeroSummary(CurrencyCode.COP),
    val groups: List<DayGroup> = emptyList(),
    val loading: Boolean = true,
    val error: MovementsError? = null,
    val ratesUnavailable: Boolean = false,
    val rangeDraft: RangeDraft? = null,
    val toast: MessageKey? = null,
)

/** Las tres pills de la lista. Solo filtran lo que se ensena, no lo que se pide a la API. */
enum class MovementFilter {
    ALL,
    INCOME,
    EXPENSE,
    ;

    /** True si un movimiento de tipo [kind] entra en este filtro. */
    fun accepts(kind: MovementKind): Boolean = when (this) {
        ALL -> true
        INCOME -> kind == MovementKind.IN
        EXPENSE -> kind == MovementKind.OUT
    }
}

/** Lo que puede fallar en las dos pantallas de este modulo, ya sin detalle de transporte. */
enum class MovementsError {
    NETWORK,
    SESSION_EXPIRED,

    /** 409 `idempotency_key_in_progress`: el mismo intento esta en vuelo. Esperar y repetir. */
    IN_PROGRESS,

    /** 409 `idempotency_key_reused`: cambio el cuerpo, asi que hace falta una clave nueva. */
    BODY_CHANGED,

    /** Cualquier otro 409. Es el unico que se resuelve sin que el usuario haga nada. */
    CONFLICT,
    UNEXPECTED,
}

/**
 * Traduce el error de la API a lo que las pantallas saben ensenar.
 *
 * Los dos 409 de idempotencia se distinguen porque piden cosas distintas: uno esperar y repetir,
 * y el otro volver a pulsar Guardar. Aplastarlos en un mensaje generico dejaba al usuario sin
 * saber cual de las dos.
 */
fun movementsErrorOf(error: ApiError): MovementsError = when (error) {
    is ApiError.Network -> MovementsError.NETWORK
    is ApiError.Unauthorized -> MovementsError.SESSION_EXPIRED
    is ApiError.Conflict -> conflictErrorOf(error.code)
    else -> MovementsError.UNEXPECTED
}

private fun conflictErrorOf(code: String?): MovementsError = when (code) {
    ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS -> MovementsError.IN_PROGRESS
    ConflictCode.IDEMPOTENCY_KEY_REUSED -> MovementsError.BODY_CHANGED
    else -> MovementsError.CONFLICT
}

/** Un resumen a cero, para el primer fotograma y para un periodo sin movimientos. */
fun zeroSummary(currency: CurrencyCode): PeriodSummary = PeriodSummary(
    income = Money.zero(currency),
    expense = Money.zero(currency),
    net = Money.zero(currency),
)
