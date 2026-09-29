import { describe, expect, it } from 'vitest';

import {
  applyRange,
  canApplyRange,
  canGoForward,
  closeRangeSheet,
  editFrom,
  editTo,
  initialFilter,
  isMonthSelection,
  nextMonthOrClearRange,
  openRangeSheet,
  previousMonth,
  selectPreset,
  type PeriodFilterState,
} from './period-filter';

const TODAY = '2026-09-22';

function opened(): PeriodFilterState {
  return openRangeSheet(initialFilter(TODAY));
}

describe('filtro de periodo', () => {
  it('arranca en el mes de hoy y con el sheet cerrado', () => {
    const state = initialFilter(TODAY);
    expect(state.selection.period).toEqual({ kind: 'month', month: '2026-09' });
    expect(state.draft).toBeNull();
    expect(isMonthSelection(state.selection)).toBe(true);
  });

  it('retrocede sin tope y no se pasa del mes de hoy', () => {
    const back = previousMonth(previousMonth(initialFilter(TODAY)));
    expect(back.selection.period).toEqual({ kind: 'month', month: '2026-07' });
    expect(canGoForward(back.selection, TODAY)).toBe(true);

    const current = initialFilter(TODAY);
    expect(canGoForward(current.selection, TODAY)).toBe(false);
    expect(nextMonthOrClearRange(current, TODAY)).toBe(current);
  });

  it('con un rango activo, el boton de la derecha vuelve al mes de hoy', () => {
    const range = applyRange(selectPreset(opened(), 'last-month', TODAY));
    expect(range.selection.period.kind).toBe('range');

    const cleared = nextMonthOrClearRange(range, TODAY);
    expect(cleared.selection.period).toEqual({ kind: 'month', month: '2026-09' });
    expect(cleared.selection.preset).toBeNull();
  });

  /** Abrir el sheet sobre un rango ya aplicado y ver otras fechas hace pensar que el filtro es otro. */
  it('el sheet se siembra con el periodo vigente', () => {
    const state = opened();
    expect(state.draft).toEqual({ from: '2026-09-01', to: '2026-09-30', preset: null });
  });

  it('una pill solo rellena el borrador: el periodo no cambia hasta aplicar', () => {
    const chosen = selectPreset(opened(), 'last-month', TODAY);
    expect(chosen.draft).toEqual({ from: '2026-08-01', to: '2026-08-31', preset: 'last-month' });
    expect(chosen.selection.period).toEqual({ kind: 'month', month: '2026-09' });

    const applied = applyRange(chosen);
    expect(applied.selection).toEqual({
      period: { kind: 'range', from: '2026-08-01', to: '2026-08-31' },
      preset: 'last-month',
    });
    expect(applied.draft).toBeNull();
  });

  it('teclear una fecha a mano desmarca la pill pero conserva lo tecleado', () => {
    const edited = editTo(
      editFrom(selectPreset(opened(), 'this-year', TODAY), '2026-03-01'),
      '2026-04-01',
    );
    expect(edited.draft).toEqual({ from: '2026-03-01', to: '2026-04-01', preset: null });
  });

  it('un rango al reves no se puede aplicar', () => {
    const backwards = editFrom(opened(), '2026-12-01');
    expect(canApplyRange(backwards.draft)).toBe(false);
    expect(applyRange(backwards)).toBe(backwards);
  });

  /** Cerrar tira el borrador: al reabrir se vuelve a sembrar del periodo. */
  it('cerrar sin aplicar tira el borrador', () => {
    const closed = closeRangeSheet(selectPreset(opened(), 'this-year', TODAY));
    expect(closed.draft).toBeNull();
    expect(openRangeSheet(closed).draft?.preset).toBeNull();
  });

  it('con un rango activo la flecha izquierda no hace nada', () => {
    const range = applyRange(selectPreset(opened(), 'last-month', TODAY));
    expect(previousMonth(range)).toBe(range);
    expect(canGoForward(range.selection, TODAY)).toBe(false);
  });

  it('editar sin sheet abierto no toca nada', () => {
    const state = initialFilter(TODAY);
    expect(editFrom(state, '2026-01-01')).toBe(state);
    expect(editTo(state, '2026-01-01')).toBe(state);
    expect(canApplyRange(null)).toBe(false);
  });
});
