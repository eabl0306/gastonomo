package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.state.PeriodFilter
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.movement.CreateMovement
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings

/**
 * Lo que necesitan las pantallas de este modulo. Lo implementa la raiz de composicion de
 * la aplicacion, en su propio fichero: este modulo no conoce a ningun adaptador.
 *
 * Si una pantalla nueva necesita otro caso de uso, se anade aqui y se rellena en
 * `MovementsWiring.kt` de `:app`. Son los dos unicos ficheros que hay que tocar, y los dos son
 * de este modulo o suyos, asi que nadie mas los esta editando.
 */
interface MovementsDependencies {
    val loadPeriodMovements: LoadPeriodMovements

    val createMovement: CreateMovement

    val loadCategories: LoadCategories

    val loadCurrencyCatalog: LoadCurrencyCatalog

    val loadExchangeRates: LoadExchangeRates

    val loadSettings: LoadSettings

    /**
     * El filtro de fechas, que es **el mismo objeto** que recibe `:feature:home`: el diseno dice
     * que el periodo elegido se ve en las dos pantallas.
     */
    val periodFilter: PeriodFilter

    /** El toast que deja el formulario al guardar y volver atras. */
    val messages: TransientMessages

    val clock: Clock
}
