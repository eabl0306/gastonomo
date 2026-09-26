package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Lo que necesita el formulario de Nuevo movimiento antes de poder pintarse. */
data class NewMovementForm(
    val displayCurrency: CurrencyCode,
    val rates: ExchangeRates,
    val categories: List<Category>,
    val currencies: List<CurrencyInfo>,
)

/** El resultado de cargar la lista una vez: lo que se pudo traer y lo que fallo. */
data class MovementsLoad(
    val displayCurrency: CurrencyCode,
    val rates: ExchangeRates,
    val movements: List<Movement>?,
    val summary: PeriodSummary?,
    val error: MovementsError?,
    val ratesUnavailable: Boolean,
)

/**
 * Trae los movimientos del periodo y el resumen de tres columnas.
 *
 * La lista se pide **sin filtrar por tipo** a proposito: el resumen se calcula sobre el periodo
 * entero y las pills filtran en local. Se agota el cursor, porque para un rango largo la
 * primera pagina no es todo el periodo y el resumen saldria corto.
 */
internal class MovementsLoader(private val dependencies: MovementsDependencies) {

    suspend fun load(period: Period): MovementsLoad = coroutineScope {
        val settings = async { dependencies.loadSettings() }
        val rates = async { dependencies.loadExchangeRates() }
        val movements = async { dependencies.loadPeriodMovements(period) }
        val display = settings.await().valueOrNull()?.displayCurrency ?: UserSettings().displayCurrency
        val table = rates.await()
        val exchange = table.valueOrNull() ?: ExchangeRates.EMPTY
        val loaded = movements.await()
        MovementsLoad(
            displayCurrency = display,
            rates = exchange,
            movements = loaded.valueOrNull(),
            summary = loaded.valueOrNull()?.let { PeriodSummary.of(it, display, exchange) },
            error = (loaded as? Outcome.Failure)?.let { movementsErrorOf(it.error) },
            ratesUnavailable = table is Outcome.Failure,
        )
    }

    /**
     * Los ajustes, las tasas, las categorias y el catalogo de monedas del formulario.
     *
     * Ninguna de las cuatro rompe la pantalla: en arranque en frio el catalogo llega vacio y el
     * sheet de moneda sale sin nada, pero registrar un movimiento funciona igual.
     */
    suspend fun loadForm(): NewMovementForm = coroutineScope {
        val settings = async { dependencies.loadSettings() }
        val rates = async { dependencies.loadExchangeRates() }
        val categories = async { dependencies.loadCategories() }
        val currencies = async { dependencies.loadCurrencyCatalog() }
        NewMovementForm(
            displayCurrency = settings.await().valueOrNull()?.displayCurrency
                ?: UserSettings().displayCurrency,
            rates = rates.await().valueOrNull() ?: ExchangeRates.EMPTY,
            categories = categories.await().valueOrNull().orEmpty(),
            currencies = currencies.await().valueOrNull().orEmpty(),
        )
    }
}
