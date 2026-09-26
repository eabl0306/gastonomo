package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.feature.home.HomeDependencies

/**
 * Lo que `:feature:home` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun homeDependencies(useCases: UseCases): HomeDependencies =
    object : HomeDependencies {
        override val loadPeriodMovements = useCases.loadPeriodMovements
        override val loadRecentMovements = useCases.loadRecentMovements
        override val loadLoans = useCases.loadLoans
        override val loadExchangeRates = useCases.loadExchangeRates
        override val loadSettings = useCases.loadSettings
        override val loadProfile = useCases.loadProfile
        override val setHideAmounts = useCases.setHideAmounts
        override val observeDeviceSettings = useCases.observeDeviceSettings

        // El filtro de fechas y el toast son compartidos con :feature:movements: tienen que
        // ser el mismo objeto en las dos, no una copia por feature.
        override val periodFilter = sharedPeriodFilter
        override val messages = sharedMessages
        override val clock = useCases.clock
    }
