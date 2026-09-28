package com.bahoque.gastonomo.feature.home

import com.bahoque.gastonomo.core.application.state.MessageKey
import com.bahoque.gastonomo.core.application.state.PeriodSelection
import com.bahoque.gastonomo.core.application.state.RangeDraft
import com.bahoque.gastonomo.core.domain.analysis.BalanceTrend
import com.bahoque.gastonomo.core.domain.analysis.CategoryBreakdown
import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.user.User
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Todo lo que pinta Inicio, inmutable y en un solo objeto.
 *
 * El periodo no se guarda aqui como fuente de verdad: lo manda
 * [com.bahoque.gastonomo.core.application.state.PeriodFilter], que es el mismo que ve
 * Movimientos. Esta copia solo existe para pintar el selector.
 *
 * @property ratesUnavailable `GET /v1/exchange-rates` respondio 503 porque la tarea de
 *   refresco aun no consiguio una pasada. No es un fallo de la pantalla: lo que esta en la
 *   moneda principal se ensena igual y se avisa con una linea.
 */
data class HomeUiState(
    val today: LocalDate,
    val selection: PeriodSelection,
    val displayCurrency: CurrencyCode = CurrencyCode.COP,
    val rates: ExchangeRates = ExchangeRates.EMPTY,
    val user: User? = null,
    val hideAmounts: Boolean = false,
    val loading: Boolean = true,

    /**
     * Si alguna vez llego contenido.
     *
     * Separa "todavia no hay nada que ensenar" de "estoy recargando lo que ya se ve". Sin
     * esto, cambiar de mes en una cuenta vacia cambiaba la pantalla entera por el bloque de
     * carga y la devolvia, que es el parpadeo.
     */
    val everLoaded: Boolean = false,
    val error: HomeError? = null,
    val ratesUnavailable: Boolean = false,
    val content: HomeContent = HomeContent(),
    val rangeDraft: RangeDraft? = null,
    val toast: MessageKey? = null,
) {
    /** El sheet de rango esta abierto cuando hay un borrador de fechas. */
    val rangeSheetOpen: Boolean get() = rangeDraft != null
}

/** Lo que sale de la lista de movimientos del periodo y de los prestamos. */
data class HomeContent(
    val summary: PeriodSummary = zeroSummary(CurrencyCode.COP),
    val trend: BalanceTrend = BalanceTrend(emptyList()),
    val breakdown: CategoryBreakdown = emptyBreakdown(CurrencyCode.COP),
    val recent: List<Movement> = emptyList(),
    val owedToMe: LoanTotal = LoanTotal(),
    val iOwe: LoanTotal = LoanTotal(),
)

/** El saldo pendiente y el conteo de una de las dos tarjetas de prestamos. */
data class LoanTotal(
    val outstanding: BigDecimal = BigDecimal.ZERO,
    val count: Int = 0,
)

/** Lo que puede fallar al cargar Inicio, ya sin detalle de transporte. */
enum class HomeError {
    NETWORK,
    SESSION_EXPIRED,
    UNEXPECTED,
}

/** Un resumen a cero, para el primer fotograma y para un periodo sin movimientos. */
fun zeroSummary(currency: CurrencyCode): PeriodSummary = PeriodSummary(
    income = Money.zero(currency),
    expense = Money.zero(currency),
    net = Money.zero(currency),
)

/** Un grafico de gastos vacio. */
fun emptyBreakdown(currency: CurrencyCode): CategoryBreakdown =
    CategoryBreakdown(total = Money.zero(currency), slices = emptyList())
