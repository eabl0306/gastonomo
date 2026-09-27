package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.loan.CreateLoan
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoanDetail
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.loan.RegisterLoanPayment
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.sync.ObserveConnection

/**
 * Lo que necesitan las pantallas de este modulo. Lo implementa la raiz de composicion de
 * la aplicacion, en su propio fichero: este modulo no conoce a ningun adaptador.
 *
 * Si una pantalla nueva necesita otro caso de uso, se anade aqui y se rellena en
 * `LoansWiring.kt` de `:app`. Son los dos unicos ficheros que hay que tocar, y los dos son
 * de este modulo o suyos, asi que nadie mas los esta editando.
 */
interface LoansDependencies {
    val loadLoans: LoadLoans

    val loadLoanDetail: LoadLoanDetail

    val createLoan: CreateLoan

    val registerLoanPayment: RegisterLoanPayment

    val loadCurrencyCatalog: LoadCurrencyCatalog

    val loadExchangeRates: LoadExchangeRates

    val loadSettings: LoadSettings

    val observeConnection: ObserveConnection

    val clock: Clock
}
