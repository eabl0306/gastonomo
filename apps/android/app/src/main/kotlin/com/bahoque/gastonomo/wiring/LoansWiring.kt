package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.feature.loans.LoansDependencies

/**
 * Lo que `:feature:loans` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun loansDependencies(useCases: UseCases): LoansDependencies =
    object : LoansDependencies {
        override val loadLoans = useCases.loadLoans
        override val loadLoanDetail = useCases.loadLoanDetail
        override val createLoan = useCases.createLoan
        override val registerLoanPayment = useCases.registerLoanPayment
        override val loadCurrencyCatalog = useCases.loadCurrencyCatalog
        override val loadExchangeRates = useCases.loadExchangeRates
        override val loadSettings = useCases.loadSettings
        override val clock = useCases.clock
    }
