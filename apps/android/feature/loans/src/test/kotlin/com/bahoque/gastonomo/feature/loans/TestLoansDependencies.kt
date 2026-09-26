package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.loan.CreateLoan
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoanDetail
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.loan.RegisterLoanPayment
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.testing.fake.FakeCurrencyGateway
import com.bahoque.gastonomo.core.testing.fake.FakeLoanGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSettingsGateway
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance

/**
 * Las dependencias de esta feature con un unico doble de prestamos detras, para poder mirar
 * las claves de idempotencia que recibio.
 *
 * Todo lo demas usa los datos de ejemplo del prototipo: asi lo que se asierta es comparable
 * con el diseno entregado.
 */
internal class TestLoansDependencies(
    val gateway: FakeLoanGateway = FakeLoanGateway(),
    currencies: FakeCurrencyGateway = FakeCurrencyGateway(),
    settings: FakeSettingsGateway = FakeSettingsGateway(),
) : LoansDependencies {
    override val loadLoans = LoadLoans(gateway)
    override val loadLoanDetail = LoadLoanDetail(gateway)
    override val createLoan = CreateLoan(gateway)
    override val registerLoanPayment = RegisterLoanPayment(gateway)
    override val loadCurrencyCatalog = LoadCurrencyCatalog(currencies)
    override val loadExchangeRates = LoadExchangeRates(currencies)
    override val loadSettings = LoadSettings(settings)
    override val clock = FixedClock()

    companion object {

        /** Dependencias cuyo puerto de prestamos falla siempre con [error]. */
        fun failing(error: ApiError, loans: List<Loan> = SampleFinance.loans) =
            TestLoansDependencies(FakeLoanGateway(loans = loans, failure = error))
    }
}
