package com.bahoque.gastonomo.feature.movements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bahoque.gastonomo.core.application.state.PeriodFilterEditor
import com.bahoque.gastonomo.core.application.state.PeriodSelection
import com.bahoque.gastonomo.core.domain.analysis.DayGrouping
import com.bahoque.gastonomo.core.domain.movement.Movement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El estado de la lista de Movimientos.
 *
 * El periodo es el mismo que ve Inicio: vive en
 * [com.bahoque.gastonomo.core.application.state.PeriodFilter] y lo maneja [period]. Las pills,
 * en cambio, son de esta pantalla y no se comparten.
 */
class MovementsViewModel(private val dependencies: MovementsDependencies) : ViewModel() {

    private val loader = MovementsLoader(dependencies)

    /** El selector de mes y el sheet de rango, compartidos con Inicio. */
    val period = PeriodFilterEditor(dependencies.periodFilter, dependencies.clock)

    private var loaded: List<Movement> = emptyList()

    /** Si ya paso por primer plano una vez. Ver [onResumed]. */
    private var resumedBefore = false

    private val mutable = MutableStateFlow(
        MovementsUiState(
            today = dependencies.clock.today(),
            selection = dependencies.periodFilter.selection.value,
        ),
    )

    val state: StateFlow<MovementsUiState> = combine(mutable, period.draft) { state, draft ->
        state.copy(rangeDraft = draft)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, mutable.value)

    init {
        viewModelScope.launch {
            dependencies.periodFilter.selection.collectLatest { selection ->
                mutable.update { it.copy(selection = selection) }
                reload(selection)
            }
        }
        viewModelScope.launch {
            dependencies.messages.message.collect { key ->
                mutable.update { it.copy(toast = key) }
            }
        }
    }

    /** Las pills no vuelven a pedir nada: la lista ya esta en memoria sin filtrar. */
    fun onFilter(filter: MovementFilter) {
        mutable.update { it.copy(filter = filter, groups = groupsOf(it.copy(filter = filter))) }
    }

    /**
     * La pantalla volvio a primer plano, asi que se vuelve a cargar.
     *
     * Hace falta porque los formularios guardan y **vuelven atras**, y esta entrada de la pila
     * nunca se destruyo: su ViewModel sigue vivo con la lista que cargo antes. Sin esto,
     * registrar un gasto ensena el toast "Gasto guardado" y la lista de abajo no cambia.
     *
     * Con la cache en memoria detras no cuesta nada: si nada se escribio, la recarga se sirve
     * de memoria y no sale ni una peticion. Si se escribio, la escritura ya invalido lo que
     * toca y esta recarga trae lo nuevo.
     *
     * La primera vez no hace nada: el bloque de inicializacion ya esta cargando, y cargar dos
     * veces a la vez en arranque frio son dos peticiones de todo.
     */
    fun onResumed() {
        if (!resumedBefore) {
            resumedBefore = true
            return
        }
        viewModelScope.launch { reload(mutable.value.selection) }
    }

    fun onRetry() {
        viewModelScope.launch { reload(mutable.value.selection) }
    }

    /** Lo llama el toast al ocultarse a los 2,2 segundos. */
    fun onToastHidden() {
        dependencies.messages.consume()
    }

    private suspend fun reload(selection: PeriodSelection) {
        mutable.update { it.copy(loading = true, error = null) }
        val load = loader.load(selection.period)
        loaded = load.movements ?: loaded
        mutable.update { state ->
            val next = state.copy(
                loading = false,
                displayCurrency = load.displayCurrency,
                rates = load.rates,
                summary = load.summary ?: state.summary,
                error = load.error,
                ratesUnavailable = load.ratesUnavailable,
            )
            next.copy(groups = groupsOf(next))
        }
    }

    /** Agrupa por dia lo que pasa el filtro, del dia mas reciente al mas antiguo. */
    private fun groupsOf(state: MovementsUiState) = DayGrouping.of(
        movements = loaded.filter { state.filter.accepts(it.kind) },
        display = state.displayCurrency,
        rates = state.rates,
    )

    companion object {
        fun factory(dependencies: MovementsDependencies): ViewModelProvider.Factory = viewModelFactory {
            initializer { MovementsViewModel(dependencies) }
        }
    }
}
