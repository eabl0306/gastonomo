package com.bahoque.gastonomo.core.application.state

import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Las fechas que se estan eligiendo en el sheet de rango, antes de aplicarlas. */
data class RangeDraft(
    val from: LocalDate,
    val to: LocalDate,
    val preset: PeriodPreset? = null,
)

/**
 * Todo lo que el usuario puede hacer con el filtro de fechas: mover el mes, abrir el sheet de
 * rango, elegir un preset, tocar las fechas a mano y aplicarlas.
 *
 * Esta fuera de los dos ViewModel porque el selector es exactamente el mismo en Inicio y en
 * Movimientos, y tener dos copias de estas ocho reglas era la forma mas facil de que las dos
 * pantallas acabaran comportandose distinto.
 *
 * ```
 * // en el ViewModel
 * val period = PeriodFilterEditor(dependencies.periodFilter, dependencies.clock)
 * // y en la pantalla
 * onPrevious = viewModel.period::previousMonth
 * ```
 */
class PeriodFilterEditor(
    private val filter: PeriodFilter,
    private val clock: Clock,
) {

    private val mutable = MutableStateFlow<RangeDraft?>(null)

    /** El borrador del sheet de rango. null significa que el sheet esta cerrado. */
    val draft: StateFlow<RangeDraft?> = mutable.asStateFlow()

    fun previousMonth() {
        filter.select(filter.selection.value.previousMonth())
    }

    /**
     * La flecha derecha hace dos cosas segun el modo: con un mes avanza, y con un rango activo
     * es el `close` que vuelve al mes de hoy. Nunca pasa del mes actual.
     */
    fun nextMonthOrClearRange() {
        val current = filter.selection.value
        val today = clock.today()
        val next = when {
            current.isRange -> PeriodSelection.currentMonth(today)
            current.canGoForward(today) -> current.nextMonth()
            else -> return
        }
        filter.select(next)
    }

    /** Abre el sheet con las fechas del periodo vigente ya puestas. */
    fun openRangeSheet() {
        val current = filter.selection.value
        mutable.value = RangeDraft(current.period.from, current.period.to, current.preset)
    }

    fun closeRangeSheet() {
        mutable.value = null
    }

    fun selectPreset(preset: PeriodPreset) {
        val range = preset.rangeOn(clock.today())
        mutable.value = RangeDraft(range.from, range.to, preset)
    }

    /** Editar una fecha a mano deselecciona el preset, igual que en el prototipo. */
    fun editFrom(from: LocalDate) {
        mutable.update { draft -> draft?.copy(from = from, preset = null) }
    }

    fun editTo(to: LocalDate) {
        mutable.update { draft -> draft?.copy(to = to, preset = null) }
    }

    /** Solo aplica si Desde es menor o igual que Hasta, como pide el diseno. */
    fun applyRange() {
        val draft = mutable.value ?: return
        if (draft.from.isAfter(draft.to)) return
        mutable.value = null
        filter.select(PeriodSelection(Period.Range(draft.from, draft.to), draft.preset))
    }
}
