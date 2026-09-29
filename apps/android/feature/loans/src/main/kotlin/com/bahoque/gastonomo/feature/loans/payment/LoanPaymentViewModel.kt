package com.bahoque.gastonomo.feature.loans.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.usecase.loan.RegisterLoanPayment
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.designsystem.format.AmountInput
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.feature.loans.LoanSaveFailure
import com.bahoque.gastonomo.feature.loans.MoneyContext
import com.bahoque.gastonomo.feature.loans.MoneyEntryState
import com.bahoque.gastonomo.feature.loans.burnsIdempotencyKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

/**
 * El sheet de Registrar abono, que viven las dos pantallas de prestamos: la lista lo abre
 * desde una tarjeta y el detalle desde su boton primario. Es un ViewModel propio para que
 * las dos pantallas compartan el sheet sin duplicar su logica.
 *
 * No carga nada: quien lo abre ya tiene el prestamo, su saldo y las tasas, y se los pasa en
 * [open]. Asi abrir el sheet no dispara ni una llamada mas.
 */
class LoanPaymentViewModel(
    private val registerLoanPayment: RegisterLoanPayment,
    private val clock: Clock,
) : ViewModel() {

    private val mutable = MutableStateFlow(PaymentUiState())

    val state: StateFlow<PaymentUiState> = mutable.asStateFlow()

    /** Abre el sheet para [loan] con el saldo y las tasas que ya tiene quien lo abre. */
    fun open(loan: Loan, summary: LoanSummary, context: MoneyContext) {
        mutable.value = PaymentUiState(
            loan = loan,
            outstanding = summary.outstanding,
            entry = MoneyEntryState.of(context.display, context.canConvert),
            date = clock.today(),
            context = context,
        )
    }

    /** Cierra el sheet y olvida lo escrito, igual que tocar el velo en el prototipo. */
    fun dismiss() {
        mutable.value = PaymentUiState()
    }

    /**
     * Aplica un cambio al importe, la moneda, la equivalencia o el sheet de moneda. Las
     * acciones concretas las arma [com.bahoque.gastonomo.feature.loans.moneyEntryActions].
     *
     * Cerrar el sheet de moneda no toca nada mas, asi que se vuelve a este sheet con todo lo
     * escrito en su sitio.
     */
    fun editEntry(transform: MoneyEntryState.() -> MoneyEntryState) =
        mutable.update { it.copy(entry = it.entry.transform(), failure = null) }

    /**
     * Rellena el importe con el saldo completo o con la mitad, convertido a la moneda que
     * este elegida en el sheet. Borra la equivalencia editada: la sugerida vuelve a valer.
     */
    fun fill(portion: PaymentPortion) = mutable.update { current ->
        val target = portion.of(current.outstanding.amount)
        val converted = current.context.rates
            .convert(target, current.outstanding.currency, current.entry.currency)
            ?: target
        current.copy(
            entry = current.entry
                .withAmount(AmountInput.toRaw(converted, current.entry.currency))
                .withRate(null),
        )
    }

    fun onDate(date: LocalDate) = mutable.update { it.copy(date = date, failure = null) }

    /**
     * Guarda el abono con la clave de este intento, la misma en todos los reintentos.
     *
     * Un 422 no quema la clave: se corrige el importe y se vuelve a pulsar con la misma. Un
     * 409 `idempotency_key_reused` si, porque significa que el cuerpo cambio.
     *
     * @param onSaved se llama con el importe tal como se teclo, para el toast, y solo
     *   cuando la API creo el abono de verdad: un reintento ya se pinto la primera vez.
     */
    fun save(onSaved: (Money) -> Unit) {
        val current = mutable.value
        val loan = current.loan ?: return
        val suggestion = current.entry.suggestion(current.context)
        if (!current.canSave) return
        val key = current.idempotencyKey ?: IdempotencyKey.newKey()
        mutable.update { it.copy(saving = true, failure = null, idempotencyKey = key) }
        viewModelScope.launch {
            val draft = LoanPaymentDraft(paidOn = current.date, money = suggestion.money)
            when (val result = registerLoanPayment(loan.id, draft, key)) {
                is Outcome.Success -> {
                    if (!result.value.isReplay) onSaved(suggestion.money.original)
                    dismiss()
                }
                is Outcome.Failure -> mutable.update { current ->
                    val failure = LoanSaveFailure.of(result.error)
                    current.copy(
                        saving = false,
                        failure = failure,
                        idempotencyKey = current.idempotencyKey.takeUnless { failure.burnsIdempotencyKey() },
                    )
                }
            }
        }
    }
}

/** Lo que rellenan los dos chips del sheet. */
enum class PaymentPortion {
    /** "Todo el saldo". */
    ALL,

    /** "La mitad". */
    HALF,
    ;

    /** El trozo de [balance] que le toca. */
    fun of(balance: BigDecimal): BigDecimal = when (this) {
        ALL -> balance
        HALF -> balance.divide(TWO, ExchangeRates.PRECISION)
    }

    private companion object {
        val TWO = BigDecimal(2)
    }
}
