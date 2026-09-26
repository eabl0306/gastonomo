package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.state.PeriodFilter
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.common.DispatcherProvider

/**
 * Lo que necesitan las pantallas de este modulo. Lo implementa la raiz de composicion de
 * la aplicacion, en su propio fichero: este modulo no conoce a ningun adaptador.
 *
 * Si una pantalla nueva necesita otro caso de uso, se anade aqui y se rellena en
 * `ExportWiring.kt` de `:app`. Son los dos unicos ficheros que hay que tocar, y los dos son
 * de este modulo o suyos, asi que nadie mas los esta editando.
 */
interface ExportDependencies {
    val loadPeriodMovements: LoadPeriodMovements

    val loadLoans: LoadLoans

    val loadSettings: LoadSettings

    val loadExchangeRates: LoadExchangeRates

    /**
     * El mismo filtro de fechas de Inicio y de Movimientos. "Periodo actual" es el periodo que
     * el usuario esta mirando, no el mes de hoy: si venia de un rango, exporta ese rango.
     */
    val periodFilter: PeriodFilter

    val clock: Clock

    /** Escribir el archivo no puede ir en el hilo principal: un historial largo tarda. */
    val dispatchers: DispatcherProvider
}
