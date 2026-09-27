package com.bahoque.gastonomo.feature.settings.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.CorrectPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.DiscardPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.ObservePendingWrites
import com.bahoque.gastonomo.core.application.usecase.sync.RetryPendingWrites
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

/**
 * Los registros que todavia no han salido del telefono.
 *
 * Lo unico que se puede corregir aqui es el importe, que es lo que la API rechaza cuando rechaza
 * algo. El resto se ensena tal cual: cambiarlo seria escribir otro registro, no corregir este.
 */
class PendingWritesViewModel(
    private val observe: ObservePendingWrites,
    private val correct: CorrectPendingWrite,
    private val discard: DiscardPendingWrite,
    private val retry: RetryPendingWrites,
) : ViewModel() {

    private val mutable = MutableStateFlow(PendingWritesUiState())

    val state: StateFlow<PendingWritesUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            observe().collect { writes -> mutable.update { it.copy(writes = writes) } }
        }
    }

    fun onEdit(write: PendingWrite) = mutable.update {
        it.copy(editing = write.id, amount = amountOf(write).toPlainString())
    }

    fun onAmount(text: String) = mutable.update { it.copy(amount = text.filter(::isAmountChar)) }

    fun onCancelEdit() = mutable.update { it.copy(editing = null, amount = "") }

    /** Guarda el importe corregido y vuelve a intentarlo. La clave de idempotencia no cambia. */
    fun onSave() {
        val current = mutable.value
        val write = current.writes.firstOrNull { it.id == current.editing } ?: return
        val amount = current.amount.toBigDecimalOrNull() ?: return
        mutable.update { it.copy(editing = null, amount = "") }
        viewModelScope.launch { correct(write.id, write.draft.withAmount(amount)) }
    }

    fun onAskDiscard(write: PendingWrite) = mutable.update { it.copy(confirming = write.id) }

    fun onCancelDiscard() = mutable.update { it.copy(confirming = null) }

    fun onConfirmDiscard() {
        val id = mutable.value.confirming ?: return
        mutable.update { it.copy(confirming = null) }
        viewModelScope.launch { discard(id) }
    }

    fun onRetry() {
        viewModelScope.launch { retry() }
    }

    private fun isAmountChar(char: Char) = char.isDigit() || char == '.'
}

/** El importe de una escritura, sea del tipo que sea. */
internal fun amountOf(write: PendingWrite): BigDecimal = moneyOf(write).amount

internal fun moneyOf(write: PendingWrite): MonetaryAmount = when (val draft = write.draft) {
    is PendingDraft.Movement -> draft.value.money
    is PendingDraft.Loan -> draft.value.money
    is PendingDraft.Payment -> draft.value.money
}

/**
 * Cambia el importe conservando la moneda y la equivalencia proporcionalmente.
 *
 * La equivalencia se recalcula con la misma tasa que tenia el registro, no con la de ahora: es el
 * mismo gasto del mismo dia, solo que con el importe corregido.
 */
internal fun PendingDraft.withAmount(amount: BigDecimal): PendingDraft = when (this) {
    is PendingDraft.Movement -> PendingDraft.Movement(value.copy(money = value.money.at(amount)))
    is PendingDraft.Loan -> PendingDraft.Loan(value.copy(money = value.money.at(amount)))
    is PendingDraft.Payment -> PendingDraft.Payment(value.copy(money = value.money.at(amount)))
}

private fun MonetaryAmount.at(newAmount: BigDecimal): MonetaryAmount {
    if (amount.signum() == 0) return copy(amount = newAmount, base = newAmount)
    val rate = base.divide(amount, MonetaryAmount.CORRECTION_PRECISION)
    return copy(amount = newAmount, base = newAmount.multiply(rate, MonetaryAmount.CORRECTION_PRECISION))
}
