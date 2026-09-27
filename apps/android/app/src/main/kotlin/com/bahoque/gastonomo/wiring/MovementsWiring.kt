package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.feature.movements.MovementsDependencies

/**
 * Lo que `:feature:movements` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun movementsDependencies(useCases: UseCases): MovementsDependencies =
    object : MovementsDependencies {
        override val loadPeriodMovements = useCases.loadPeriodMovements
        override val createMovement = useCases.createMovement
        override val loadCategories = useCases.loadCategories
        override val loadCurrencyCatalog = useCases.loadCurrencyCatalog
        override val loadExchangeRates = useCases.loadExchangeRates
        override val loadSettings = useCases.loadSettings

        // El filtro de fechas y el toast son compartidos con :feature:home: tienen que ser el
        // mismo objeto en las dos, no una copia por feature.
        override val periodFilter = sharedPeriodFilter
        override val messages = sharedMessages
        override val observeConnection = useCases.observeConnection
        override val clock = useCases.clock
    }
