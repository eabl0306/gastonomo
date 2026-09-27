package com.bahoque.gastonomo.feature.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * El formulario de Nuevo movimiento, la pantalla 7.
 *
 * Al guardar bien marca `saved`: quien lo pinta vuelve atras y el toast, que deja
 * [MovementSaver], sale encima de la pantalla de destino, que es donde lo pone el diseno.
 *
 * La clave de idempotencia la lleva [MovementSaver], que es la unica pieza que la conoce.
 */
class NewMovementViewModel(
    private val dependencies: MovementsDependencies,
    kind: MovementKind,
) : ViewModel() {

    private val loader = MovementsLoader(dependencies)
    private val saver = MovementSaver(dependencies)

    private val mutable = MutableStateFlow(
        NewMovementUiState(
            today = dependencies.clock.today(),
            kind = kind,
            date = dependencies.clock.today(),
        ),
    )

    val state: StateFlow<NewMovementUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            val form = loader.loadForm()
            mutable.update {
                it.copy(
                    loading = false,
                    displayCurrency = form.displayCurrency,
                    currency = form.displayCurrency,
                    rates = form.rates,
                    categories = form.categories,
                    currencies = form.currencies,
                    canConvert = form.canConvert,
                )
            }
        }
    }

    /** Cambiar de tipo borra la categoria elegida: puede no existir en el otro grid. */
    fun onKind(kind: MovementKind) {
        mutable.update { it.copy(kind = kind, selectedCategoryId = null) }
    }

    fun onAmount(raw: String) {
        mutable.update { it.copy(amountRaw = raw, fieldErrors = emptyList()) }
    }

    /** La equivalencia de la caja de conversion. null la devuelve a la tasa sugerida. */
    fun onBase(raw: String?) {
        mutable.update { it.copy(baseRaw = raw) }
    }

    fun onCategory(id: CategoryId) {
        mutable.update { it.copy(selectedCategoryId = id) }
    }

    fun onDate(date: LocalDate) {
        mutable.update { it.copy(date = date) }
    }

    fun onNote(note: String) {
        mutable.update { it.copy(note = note) }
    }

    fun onCurrencySheet(open: Boolean) {
        mutable.update { it.copy(currencySheetOpen = open, currencyQuery = "") }
    }

    fun onCurrencyQuery(query: String) {
        mutable.update { it.copy(currencyQuery = query) }
    }

    /** Cambiar la moneda del registro borra la equivalencia editada. */
    fun onCurrency(currency: CurrencyCode) {
        mutable.update { it.copy(currency = currency, baseRaw = null, currencySheetOpen = false) }
    }

    fun onSave() {
        val current = mutable.value
        val categoryId = current.selectedCategoryId
        if (!current.canSave || categoryId == null) return
        mutable.update { it.copy(saving = true, error = null, fieldErrors = emptyList()) }
        val draft = MovementDraft(
            occurredOn = current.date,
            kind = current.kind,
            categoryId = categoryId,
            money = current.suggestion.money,
            note = current.note.takeIf { it.isNotBlank() },
        )
        viewModelScope.launch {
            when (val result = saver.save(draft)) {
                is SaveResult.Saved -> mutable.update { it.copy(saving = false, saved = true) }
                is SaveResult.Failed -> mutable.update {
                    it.copy(saving = false, error = result.error, fieldErrors = result.fieldErrors)
                }
            }
        }
    }

    companion object {
        fun factory(
            dependencies: MovementsDependencies,
            kind: MovementKind,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { NewMovementViewModel(dependencies, kind) }
        }
    }
}
