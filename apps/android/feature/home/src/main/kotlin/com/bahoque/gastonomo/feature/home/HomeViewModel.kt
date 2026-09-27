package com.bahoque.gastonomo.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bahoque.gastonomo.core.application.state.PeriodFilterEditor
import com.bahoque.gastonomo.core.application.state.PeriodSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El estado de Inicio y lo que puede hacer el usuario en ella.
 *
 * El periodo vive en [com.bahoque.gastonomo.core.application.state.PeriodFilter], que es el
 * mismo que ve Movimientos, y lo que se puede hacer con el esta en [period]: aqui solo se
 * observa y se recarga. Cada cambio vuelve a cargar el periodo entero, y `collectLatest`
 * cancela la carga anterior para que dos pulsaciones rapidas en la flecha del mes no dejen la
 * pantalla pintando el mes equivocado.
 */
class HomeViewModel(private val dependencies: HomeDependencies) : ViewModel() {

    private val loader = HomeLoader(dependencies)

    /** Si ya paso por primer plano una vez. Ver [onResumed]. */
    private var resumedBefore = false

    /** El selector de mes y el sheet de rango, compartidos con Movimientos. */
    val period = PeriodFilterEditor(dependencies.periodFilter, dependencies.clock)

    private val mutable = MutableStateFlow(
        HomeUiState(
            today = dependencies.clock.today(),
            selection = dependencies.periodFilter.selection.value,
        ),
    )

    val state: StateFlow<HomeUiState> = combine(mutable, period.draft) { state, draft ->
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
            dependencies.observeDeviceSettings().collect { device ->
                mutable.update { it.copy(hideAmounts = device.hideAmounts) }
            }
        }
        viewModelScope.launch {
            dependencies.messages.message.collect { key ->
                mutable.update { it.copy(toast = key) }
            }
        }
    }

    /** El ojo de la tarjeta de balance. Es un ajuste del dispositivo, no de la cuenta. */
    fun onToggleHiddenAmounts() {
        val hidden = !mutable.value.hideAmounts
        viewModelScope.launch { dependencies.setHideAmounts(hidden) }
    }

    /**
     * La pantalla volvio a primer plano, asi que se vuelve a cargar.
     *
     * Hace falta porque los formularios guardan y **vuelven atras**, y esta entrada de la pila
     * nunca se destruyo: su ViewModel sigue vivo con la lista que cargo antes. Sin esto,
     * registrar un gasto ensena el toast "Gasto guardado" y el balance de arriba no cambia.
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
        val load = loader.load(selection.period, mutable.value.today)
        mutable.update { state ->
            state.copy(
                loading = false,
                displayCurrency = load.displayCurrency,
                rates = load.rates,
                user = load.user ?: state.user,
                content = load.content ?: state.content,
                error = load.error,
                ratesUnavailable = load.ratesUnavailable,
            )
        }
    }

    companion object {
        /** La fabrica que usa la pantalla; la inyeccion es manual, sin Hilt. */
        fun factory(dependencies: HomeDependencies): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(dependencies) }
        }
    }
}
