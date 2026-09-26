package com.bahoque.gastonomo.feature.home

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.analysis.BalanceTrend
import com.bahoque.gastonomo.core.domain.analysis.CategoryBreakdown
import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.domain.user.User
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.time.LocalDate

/** El resultado de cargar Inicio una vez: lo que se pudo traer y lo que fallo. */
data class HomeLoad(
    val displayCurrency: CurrencyCode,
    val rates: ExchangeRates,
    val user: User?,
    val content: HomeContent?,
    val error: HomeError?,
    val ratesUnavailable: Boolean,
)

/**
 * Trae de la API todo lo que ensena Inicio y lo convierte en lo que pinta la pantalla.
 *
 * Esta fuera del ViewModel porque son seis llamadas, cuatro de ellas en paralelo, y mezclarlo
 * con el manejo del estado dejaba una funcion que nadie podia leer. El ViewModel se queda con
 * lo suyo: guardar el resultado y reaccionar a lo que toca el usuario.
 *
 * Lo unico que rompe la pantalla es que falle la lista de movimientos: de ella salen el
 * balance, la tendencia, los gastos por categoria y los ultimos movimientos. Si solo fallan
 * los ajustes, el perfil o las tasas, se ensena lo que hay.
 */
internal class HomeLoader(private val dependencies: HomeDependencies) {

    suspend fun load(period: Period, today: LocalDate): HomeLoad = coroutineScope {
        val settings = async { dependencies.loadSettings() }
        val rates = async { dependencies.loadExchangeRates() }
        val movements = async { dependencies.loadPeriodMovements(period) }
        val recent = async { dependencies.loadRecentMovements(period) }
        val profile = async { dependencies.loadProfile() }
        val display = settings.await().valueOrNull()?.displayCurrency ?: UserSettings().displayCurrency
        val table = rates.await()
        val snapshot = Snapshot(
            period = period,
            today = today,
            display = display,
            rates = table.valueOrNull() ?: ExchangeRates.EMPTY,
        )
        val loaded = movements.await()
        if (loaded is Outcome.Failure) {
            return@coroutineScope failure(display, loaded.error, table)
        }
        HomeLoad(
            displayCurrency = display,
            rates = snapshot.rates,
            user = profile.await().valueOrNull(),
            content = contentOf(
                snapshot = snapshot,
                movements = (loaded as Outcome.Success).value,
                recent = recent.await().valueOrNull().orEmpty(),
                loans = dependencies.loadLoans(snapshot.display, snapshot.rates).valueOrNull().orEmpty(),
            ),
            error = null,
            ratesUnavailable = table is Outcome.Failure,
        )
    }

    private fun failure(
        display: CurrencyCode,
        error: ApiError,
        rates: Outcome<ExchangeRates, ApiError>,
    ): HomeLoad = HomeLoad(
        displayCurrency = display,
        rates = rates.valueOrNull() ?: ExchangeRates.EMPTY,
        user = null,
        content = null,
        error = homeErrorOf(error),
        ratesUnavailable = rates is Outcome.Failure,
    )

    private fun contentOf(
        snapshot: Snapshot,
        movements: List<Movement>,
        recent: List<Movement>,
        loans: List<Pair<Loan, LoanSummary>>,
    ): HomeContent = HomeContent(
        summary = PeriodSummary.of(movements, snapshot.display, snapshot.rates),
        trend = BalanceTrend.of(movements, snapshot.period, snapshot.today, snapshot.display, snapshot.rates),
        breakdown = CategoryBreakdown.ofExpenses(movements, snapshot.display, snapshot.rates),
        recent = recent,
        owedToMe = totalOf(loans, LoanDirection.LENT),
        iOwe = totalOf(loans, LoanDirection.BORROWED),
    )

    /**
     * "Me deben" y "Debo" suman solo los prestamos con saldo y no dependen del periodo: son
     * siempre saldos actuales, tal como dice el diseno.
     */
    private fun totalOf(
        loans: List<Pair<Loan, LoanSummary>>,
        direction: LoanDirection,
    ): LoanTotal {
        val open = loans.filter { (loan, summary) ->
            loan.direction == direction && summary.outstanding.amount.signum() > 0
        }
        return LoanTotal(
            outstanding = open.fold(BigDecimal.ZERO) { total, (_, summary) -> total + summary.outstanding.amount },
            count = open.size,
        )
    }

    /** Los cuatro datos con los que se calcula todo, juntos para no encadenar parametros. */
    private data class Snapshot(
        val period: Period,
        val today: LocalDate,
        val display: CurrencyCode,
        val rates: ExchangeRates,
    )
}

/** Traduce el error de la API a lo que la pantalla sabe ensenar. */
internal fun homeErrorOf(error: ApiError): HomeError = when (error) {
    is ApiError.Network -> HomeError.NETWORK
    is ApiError.Unauthorized -> HomeError.SESSION_EXPIRED
    else -> HomeError.UNEXPECTED
}
