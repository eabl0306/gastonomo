package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.core.common.DefaultDispatcherProvider
import com.bahoque.gastonomo.feature.export.ExportDependencies

/**
 * Lo que `:feature:export` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun exportDependencies(useCases: UseCases): ExportDependencies =
    object : ExportDependencies {
        override val loadPeriodMovements = useCases.loadPeriodMovements
        override val loadLoans = useCases.loadLoans
        override val loadSettings = useCases.loadSettings
        override val loadExchangeRates = useCases.loadExchangeRates
        override val periodFilter = sharedPeriodFilter
        override val clock = useCases.clock
        override val dispatchers = DefaultDispatcherProvider
    }
