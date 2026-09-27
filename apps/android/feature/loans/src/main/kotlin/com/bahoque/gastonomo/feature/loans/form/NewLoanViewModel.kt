package com.bahoque.gastonomo.feature.loans.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.usecase.loan.CreateLoan
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.LoanSaveFailure
import com.bahoque.gastonomo.feature.loans.MoneyContextLoader
import com.bahoque.gastonomo.feature.loans.MoneyEntryState
import com.bahoque.gastonomo.feature.loans.burnsIdempotencyKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Pantalla 10. Crea el prestamo con un solo POST: la API escribe tambien su movimiento
 * ("Prestamo - {persona}", gasto si preste e ingreso si me prestaron), asi que el cliente no
 * crea ningun movimiento por su cuenta.
 */
class NewLoanViewModel(
    private val createLoan: CreateLoan,
    private val contextLoader: MoneyContextLoader,
    clock: Clock,
) : ViewModel() {

    private val mutable = MutableStateFlow(NewLoanUiState(date = clock.today()))

    val state: StateFlow<NewLoanUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            val context = contextLoader.load()
            mutable.update {
                it.copy(context = context, entry = MoneyEntryState.of(context.display, context.canConvert))
            }
        }
    }

    /** Cambiar de direccion cambia el color del monto, la etiqueta del campo y la caja de info. */
    fun onDirection(direction: LoanDirection) =
        mutable.update { it.copy(direction = direction, failure = null) }

    fun onPerson(person: String) = mutable.update { it.copy(person = person, failure = null) }

    fun onNote(note: String) = mutable.update { it.copy(note = note, failure = null) }

    fun onDate(date: LocalDate) = mutable.update { it.copy(date = date, failure = null) }

    /**
     * Aplica un cambio al importe, la moneda, la equivalencia o el sheet de moneda. Las
     * acciones concretas las arma [com.bahoque.gastonomo.feature.loans.moneyEntryActions].
     */
    fun editEntry(transform: MoneyEntryState.() -> MoneyEntryState) =
        mutable.update { it.copy(entry = it.entry.transform(), failure = null) }

    /**
     * Crea el prestamo con la clave de este intento, la misma en todos sus reintentos.
     *
     * @param onCreated se llama con true cuando la respuesta era un reintento idempotente.
     *   El formulario se cierra igual, pero el exito no se vuelve a pintar: ya se pinto.
     */
    fun save(onCreated: (replay: Boolean) -> Unit) {
        val current = mutable.value
        if (!current.canSave) return
        val key = current.idempotencyKey ?: IdempotencyKey.newKey()
        mutable.update { it.copy(saving = true, failure = null, idempotencyKey = key) }
        viewModelScope.launch {
            val draft = LoanDraft(
                person = current.person.trim(),
                direction = current.direction,
                openedOn = current.date,
                money = current.entry.suggestion(current.context).money,
                note = current.note.takeIf { it.isNotBlank() },
            )
            when (val result = createLoan(draft, key)) {
                is Outcome.Success -> {
                    mutable.update { it.copy(saving = false, idempotencyKey = null) }
                    onCreated(result.value.isReplay)
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
