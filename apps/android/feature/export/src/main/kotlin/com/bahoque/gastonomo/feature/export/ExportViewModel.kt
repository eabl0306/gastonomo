package com.bahoque.gastonomo.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.domain.period.Period
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * El estado de Exportar y la generacion de los archivos.
 *
 * Los movimientos se piden **una sola vez y de todo el historial**, agotando el cursor de
 * la API, y "Periodo actual" se filtra en local: pedirlos otra vez al cambiar de pill seria
 * recorrer el mismo cursor dos veces para un subconjunto de lo que ya se tiene.
 */
class ExportViewModel(private val dependencies: ExportDependencies) : ViewModel() {

    private val mutableState = MutableStateFlow(ExportUiState())

    val state: StateFlow<ExportUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    /** Pide los ajustes, las tasas, los prestamos y todo el historial de movimientos. */
    fun load() {
        val today = dependencies.clock.today()
        val period = dependencies.periodFilter.selection.value.period
        mutableState.update { it.copy(loading = true, today = today, period = period) }
        viewModelScope.launch {
            var next = mutableState.value.copy(loading = false, loadFailed = false)
            next = next.withSettings(dependencies.loadSettings())
            next = next.withRates(dependencies.loadExchangeRates())
            next = next.withLoans(
                dependencies.loadLoans(next.displayCurrency, next.rates),
            )
            next = next.withMovements(dependencies.loadPeriodMovements(allHistory(today)))
            mutableState.update { next }
        }
    }

    fun onFormatSelected(format: ExportFormat) = mutableState.update { it.copy(format = format) }

    fun onRangeSelected(range: ExportRange) = mutableState.update { it.copy(range = range) }

    /** Marca o desmarca un conjunto. Sin ninguno marcado el boton queda deshabilitado. */
    fun onSetToggled(set: ExportSet) {
        mutableState.update {
            it.copy(sets = if (set in it.sets) it.sets - set else it.sets + set)
        }
    }

    fun onMessageShown() = mutableState.update { it.copy(message = null) }

    /**
     * Genera los archivos. Los textos de dentro llegan ya traducidos desde la pantalla, que
     * es quien tiene los recursos.
     */
    fun onExport(labels: ExportLabels) {
        val current = mutableState.value
        if (!current.canExport) return
        mutableState.update { it.copy(exporting = true) }
        viewModelScope.launch {
            val payload = withContext(dependencies.dispatchers.default) {
                current.buildPayload(labels)
            }
            mutableState.update { it.copy(exporting = false, payload = payload) }
        }
    }

    /** La pantalla avisa de que ya escribio y compartio los archivos. */
    fun onPayloadDelivered(rows: Int) {
        mutableState.update { it.copy(payload = null, message = ExportMessage.Ready(rows)) }
    }

    /** La pantalla avisa de que no pudo escribir o compartir. */
    fun onPayloadFailed() {
        mutableState.update { it.copy(payload = null, message = ExportMessage.Failed) }
    }
}

/**
 * "Todo el historial" no es un endpoint: es un rango lo bastante ancho. Se abre en 1970 y
 * se cierra un ano por delante de hoy, porque un movimiento puede estar fechado en el
 * futuro y dejarlo fuera seria perder datos sin avisar.
 */
internal fun allHistory(today: LocalDate): Period =
    Period.Range(EXPORT_EPOCH, today.plusYears(1))

/**
 * El principio de los tiempos para la exportacion. No se usa `LocalDate.EPOCH`, que en
 * Android pide API 34 y el minimo del proyecto es 29.
 */
internal val EXPORT_EPOCH: LocalDate = LocalDate.of(1970, 1, 1)
