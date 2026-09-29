import {
  addDays,
  addMonths,
  addMonthsToDay,
  firstDayOfMonth,
  firstDayOfYear,
  isAfter,
  lastDayOfMonth,
  monthOf,
  type Day,
  type Month,
} from './day';

/**
 * El filtro de fechas que comparten Inicio y Movimientos: o un mes, o un rango.
 *
 * Decide el `from` y el `to` de `GET /v1/movements` y de que periodo salen el balance, la
 * tendencia, los gastos por categoria y el resumen de tres columnas. **No afecta a los
 * prestamos**: "Me deben" y "Debo" son siempre saldos de hoy.
 */
export type Period =
  | { readonly kind: 'month'; readonly month: Month }
  | { readonly kind: 'range'; readonly from: Day; readonly to: Day };

/** Primer dia del periodo, inclusive. */
export function periodFrom(period: Period): Day {
  return period.kind === 'month' ? firstDayOfMonth(period.month) : period.from;
}

/** Ultimo dia del periodo, inclusive. */
export function periodTo(period: Period): Day {
  return period.kind === 'month' ? lastDayOfMonth(period.month) : period.to;
}

export function monthPeriod(month: Month): Period {
  return { kind: 'month', month };
}

/**
 * Un rango elegido a mano. Lanza si empieza despues de terminar: un rango al reves no es un
 * rango peor, es uno que no se puede pintar ni consultar.
 */
export function rangePeriod(from: Day, to: Day): Period {
  if (isAfter(from, to)) throw new Error(`Un rango empieza antes de terminar: ${from} a ${to}`);
  return { kind: 'range', from, to };
}

/** El mes al que pertenece [today]. Es el estado por defecto del filtro. */
export function currentMonth(today: Day): Period {
  return monthPeriod(monthOf(today));
}

/** Los cinco presets del sheet de rango, en el orden en el que salen. */
export type PeriodPreset =
  'this-month' | 'last-month' | 'last-30-days' | 'last-3-months' | 'this-year';

export const PERIOD_PRESETS: readonly PeriodPreset[] = [
  'this-month',
  'last-month',
  'last-30-days',
  'last-3-months',
  'this-year',
];

const DAYS_IN_A_MONTH = 29;
const QUARTER_MONTHS = 3;

/** El rango que produce un preset con [today] como dia de hoy. */
export function presetRange(preset: PeriodPreset, today: Day): Period {
  const month = monthOf(today);
  switch (preset) {
    case 'this-month':
      return rangePeriod(firstDayOfMonth(month), lastDayOfMonth(month));
    case 'last-month': {
      const previous = addMonths(month, -1);
      return rangePeriod(firstDayOfMonth(previous), lastDayOfMonth(previous));
    }
    case 'last-30-days':
      return rangePeriod(addDays(today, -DAYS_IN_A_MONTH), today);
    case 'last-3-months':
      return rangePeriod(addMonthsToDay(today, -QUARTER_MONTHS), today);
    case 'this-year':
      return rangePeriod(firstDayOfYear(today), today);
  }
}
