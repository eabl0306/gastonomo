package com.bahoque.gastonomo.core.application.state

import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.YearMonth

/**
 * El periodo elegido y, cuando viene de un preset del sheet de rango, cual.
 *
 * @property preset null cuando el periodo es un mes o cuando el usuario edito las fechas a
 *   mano: editar Desde o Hasta deselecciona el preset, tal como hace el prototipo.
 */
data class PeriodSelection(
    val period: Period,
    val preset: PeriodPreset? = null,
) {
    /** True cuando hay un rango activo: el selector cambia de aspecto. */
    val isRange: Boolean get() = period is Period.Range

    /** El mes del selector, o el mes al que pertenece el inicio del rango. */
    val month: YearMonth
        get() = (period as? Period.Month)?.month ?: YearMonth.from(period.from)

    /** El mes anterior, que es lo que hace `chevron_left`. */
    fun previousMonth(): PeriodSelection = onMonth(month.minusMonths(1))

    /** El mes siguiente. Quien llama comprueba antes que no se pase del mes actual. */
    fun nextMonth(): PeriodSelection = onMonth(month.plusMonths(1))

    /** True si se puede avanzar: el selector no deja pasar del mes de [today]. */
    fun canGoForward(today: LocalDate): Boolean = month < YearMonth.from(today)

    private fun onMonth(target: YearMonth): PeriodSelection =
        PeriodSelection(Period.Month(target))

    companion object {
        /** El estado inicial: el mes al que pertenece hoy. */
        fun currentMonth(today: LocalDate): PeriodSelection =
            PeriodSelection(Period.currentMonth(today))
    }
}

/**
 * El filtro de fechas de Inicio y de Movimientos, que es **uno solo**: el diseno dice que
 * el mismo periodo se ve en las dos pantallas, asi que el estado no puede vivir en el
 * ViewModel de ninguna.
 *
 * Se construye una vez en la raiz de composicion y se inyecta en las dos features, que lo
 * observan y lo cambian:
 * ```
 * // en el ViewModel
 * dependencies.periodFilter.selection
 *     .onEach { reload(it) }
 *     .launchIn(viewModelScope)
 *
 * fun onNextMonth() = periodFilter.select(selection.nextMonth())
 * ```
 *
 * No afecta a los prestamos: "Me deben" y "Debo" son siempre saldos actuales.
 */
class PeriodFilter(initial: PeriodSelection) {

    private val mutable = MutableStateFlow(initial)

    /** El periodo vigente. Cambia para las dos pantallas a la vez. */
    val selection: StateFlow<PeriodSelection> = mutable.asStateFlow()

    /** Cambia el periodo. Lo llaman el selector de mes y el sheet de rango. */
    fun select(selection: PeriodSelection) {
        mutable.value = selection
    }

    companion object {
        /** Un filtro que arranca en el mes de [today]. */
        fun startingOn(today: LocalDate): PeriodFilter =
            PeriodFilter(PeriodSelection.currentMonth(today))
    }
}
