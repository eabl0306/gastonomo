import { describe, expect, it } from 'vitest';

import {
  PERIOD_PRESETS,
  currentMonth,
  monthPeriod,
  periodFrom,
  periodTo,
  presetRange,
  rangePeriod,
} from './period';

/** El "hoy" del prototipo. */
const TODAY = '2026-09-22';

describe('period', () => {
  it('un mes va del primero al ultimo dia', () => {
    const period = monthPeriod('2026-09');
    expect(periodFrom(period)).toBe('2026-09-01');
    expect(periodTo(period)).toBe('2026-09-30');
  });

  it('el estado por defecto es el mes de hoy', () => {
    expect(currentMonth(TODAY)).toEqual(monthPeriod('2026-09'));
  });

  it('un rango es el que se le da', () => {
    const period = rangePeriod('2026-09-01', '2026-09-15');
    expect(periodFrom(period)).toBe('2026-09-01');
    expect(periodTo(period)).toBe('2026-09-15');
  });

  it('un rango al reves no se puede construir', () => {
    expect(() => rangePeriod('2026-09-15', '2026-09-01')).toThrow();
  });

  it('los cinco presets del sheet, en orden', () => {
    expect(PERIOD_PRESETS).toEqual([
      'this-month',
      'last-month',
      'last-30-days',
      'last-3-months',
      'this-year',
    ]);
  });

  it('cada preset produce su rango contra el dia de hoy', () => {
    expect(presetRange('this-month', TODAY)).toEqual(rangePeriod('2026-09-01', '2026-09-30'));
    expect(presetRange('last-month', TODAY)).toEqual(rangePeriod('2026-08-01', '2026-08-31'));
    expect(presetRange('last-30-days', TODAY)).toEqual(rangePeriod('2026-08-24', TODAY));
    expect(presetRange('last-3-months', TODAY)).toEqual(rangePeriod('2026-06-22', TODAY));
    expect(presetRange('this-year', TODAY)).toEqual(rangePeriod('2026-01-01', TODAY));
  });
});
