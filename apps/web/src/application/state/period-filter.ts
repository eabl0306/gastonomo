import { isAfter, monthOf, addMonths, type Day, type Month } from '../../domain/calendar/day';
import {
  currentMonth,
  monthPeriod,
  periodFrom,
  periodTo,
  presetRange,
  rangePeriod,
  type Period,
  type PeriodPreset,
} from '../../domain/calendar/period';

/**
 * El periodo elegido y, si salio de una pill, cual.
 *
 * El preset va **dentro** de la seleccion y no suelto: asi volver al modo mes lo apaga solo,
 * que es lo que evita una pill marcada sobre un periodo que ya no es el suyo.
 */
export interface PeriodSelection {
  readonly period: Period;
  readonly preset: PeriodPreset | null;
}

/** Lo que se esta editando en el sheet de rango. Null cuando el sheet esta cerrado. */
export interface RangeDraft {
  readonly from: Day;
  readonly to: Day;
  readonly preset: PeriodPreset | null;
}

/** El estado completo del filtro: lo elegido y el borrador del sheet. */
export interface PeriodFilterState {
  readonly selection: PeriodSelection;
  readonly draft: RangeDraft | null;
}

function monthSelection(month: Month): PeriodSelection {
  return { period: monthPeriod(month), preset: null };
}

/** El estado de arranque: el mes de hoy y el sheet cerrado. */
export function initialFilter(today: Day): PeriodFilterState {
  return { selection: { period: currentMonth(today), preset: null }, draft: null };
}

/** True si el periodo es un mes y no un rango. */
export function isMonthSelection(selection: PeriodSelection): boolean {
  return selection.period.kind === 'month';
}

/**
 * True si se puede avanzar un mes.
 *
 * No se pasa del mes de hoy: no hay datos en el futuro. Hacia atras no hay tope.
 */
export function canGoForward(selection: PeriodSelection, today: Day): boolean {
  const period = selection.period;
  return period.kind === 'month' && period.month < monthOf(today);
}

/** El mes anterior. Con un rango activo no hace nada: ahi la flecha esta apagada. */
export function previousMonth(state: PeriodFilterState): PeriodFilterState {
  const period = state.selection.period;
  if (period.kind !== 'month') return state;
  return { ...state, selection: monthSelection(addMonths(period.month, -1)) };
}

/**
 * El boton de la derecha, que hace dos cosas segun el modo: avanzar un mes, o salir del
 * rango y volver al mes de hoy.
 */
export function nextMonthOrClearRange(state: PeriodFilterState, today: Day): PeriodFilterState {
  const period = state.selection.period;
  if (period.kind !== 'month') {
    return { ...state, selection: { period: currentMonth(today), preset: null } };
  }
  if (!canGoForward(state.selection, today)) return state;
  return { ...state, selection: monthSelection(addMonths(period.month, 1)) };
}

/**
 * Abre el sheet sembrando el borrador con el periodo vigente.
 *
 * Sembrar importa: abrir el sheet sobre un rango ya aplicado y ver otras fechas hace pensar
 * que el filtro es otro.
 */
export function openRangeSheet(state: PeriodFilterState): PeriodFilterState {
  return {
    ...state,
    draft: {
      from: periodFrom(state.selection.period),
      to: periodTo(state.selection.period),
      preset: state.selection.preset,
    },
  };
}

/** Cierra el sheet y **tira** el borrador: al reabrir se vuelve a sembrar del periodo. */
export function closeRangeSheet(state: PeriodFilterState): PeriodFilterState {
  return { ...state, draft: null };
}

/** Una pill solo rellena el borrador. El periodo no cambia hasta "Aplicar rango". */
export function selectPreset(
  state: PeriodFilterState,
  preset: PeriodPreset,
  today: Day,
): PeriodFilterState {
  const range = presetRange(preset, today);
  return {
    ...state,
    draft: { from: periodFrom(range), to: periodTo(range), preset },
  };
}

/** Teclear una fecha a mano desmarca la pill, pero conserva lo tecleado. */
export function editFrom(state: PeriodFilterState, from: Day): PeriodFilterState {
  if (state.draft === null) return state;
  return { ...state, draft: { ...state.draft, from, preset: null } };
}

export function editTo(state: PeriodFilterState, to: Day): PeriodFilterState {
  if (state.draft === null) return state;
  return { ...state, draft: { ...state.draft, to, preset: null } };
}

/** True si el borrador se puede aplicar. Es lo que enciende el boton. */
export function canApplyRange(draft: RangeDraft | null): boolean {
  return draft !== null && !isAfter(draft.from, draft.to);
}

/** Aplica el rango y cierra el sheet. Un borrador al reves no hace nada. */
export function applyRange(state: PeriodFilterState): PeriodFilterState {
  const draft = state.draft;
  if (!canApplyRange(draft) || draft === null) return state;
  return {
    selection: { period: rangePeriod(draft.from, draft.to), preset: draft.preset },
    draft: null,
  };
}
