package com.bahoque.gastonomo.feature.home

import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.state.PeriodFilter
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.application.usecase.auth.LoadProfile
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.movement.LoadRecentMovements
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings
import com.bahoque.gastonomo.core.application.usecase.settings.SetHideAmounts

/**
 * Lo que necesitan las pantallas de este modulo. Lo implementa la raiz de composicion de
 * la aplicacion, en su propio fichero: este modulo no conoce a ningun adaptador.
 *
 * Si una pantalla nueva necesita otro caso de uso, se anade aqui y se rellena en
 * `HomeWiring.kt` de `:app`. Son los dos unicos ficheros que hay que tocar, y los dos son
 * de este modulo o suyos, asi que nadie mas los esta editando.
 */
interface HomeDependencies {
    val loadPeriodMovements: LoadPeriodMovements

    val loadRecentMovements: LoadRecentMovements

    val loadLoans: LoadLoans

    val loadExchangeRates: LoadExchangeRates

    val loadSettings: LoadSettings

    val loadProfile: LoadProfile

    val setHideAmounts: SetHideAmounts

    val observeDeviceSettings: ObserveDeviceSettings

    /**
     * El filtro de fechas, que es **el mismo objeto** que recibe `:feature:movements`: el
     * diseno dice que el periodo elegido se ve en las dos pantallas.
     */
    val periodFilter: PeriodFilter

    /** El toast que deja un formulario al guardar y volver atras. */
    val messages: TransientMessages

    val clock: Clock
}
