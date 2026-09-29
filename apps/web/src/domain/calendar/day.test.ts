import { describe, expect, it } from 'vitest';

import {
  addDays,
  addMonths,
  addMonthsToDay,
  compareDays,
  dayOfMonth,
  daysBetween,
  firstDayOfMonth,
  firstDayOfYear,
  isAfter,
  isBefore,
  isDay,
  lastDayOfMonth,
  monthNumber,
  monthOf,
  parseDay,
  weekdayOf,
  yearOf,
} from './day';

describe('day', () => {
  it('acepta un dia con forma y con sentido', () => {
    expect(isDay('2026-09-22')).toBe(true);
    expect(parseDay(' 2026-09-22 ')).toBe('2026-09-22');
  });

  it('rechaza lo que no es un dia, incluido el 30 de febrero', () => {
    for (const raw of ['', '2026-9-22', '22/09/2026', '2026-02-30', '2026-13-01', 'hoy']) {
      expect(isDay(raw)).toBe(false);
      expect(parseDay(raw)).toBeNull();
    }
  });

  it('compara y ordena', () => {
    expect(compareDays('2026-09-01', '2026-09-22')).toBeLessThan(0);
    expect(isBefore('2026-09-01', '2026-09-22')).toBe(true);
    expect(isAfter('2026-10-01', '2026-09-22')).toBe(true);
  });

  it('suma dias cruzando meses y anos', () => {
    expect(addDays('2026-09-30', 1)).toBe('2026-10-01');
    expect(addDays('2026-01-01', -1)).toBe('2025-12-31');
  });

  /** Sin la hora no hay horario de verano que mueva un dia al siguiente. */
  it('sumar dias no se desplaza por el cambio de hora', () => {
    expect(addDays('2026-03-28', 1)).toBe('2026-03-29');
    expect(addDays('2026-10-24', 1)).toBe('2026-10-25');
  });

  it('cuenta los dias de un rango contando los dos extremos', () => {
    expect(daysBetween('2026-09-01', '2026-09-30')).toBe(30);
    expect(daysBetween('2026-09-22', '2026-09-22')).toBe(1);
  });

  it('sabe el primero y el ultimo dia de un mes, con bisiestos', () => {
    expect(firstDayOfMonth('2026-09')).toBe('2026-09-01');
    expect(lastDayOfMonth('2026-09')).toBe('2026-09-30');
    expect(lastDayOfMonth('2026-02')).toBe('2026-02-28');
    expect(lastDayOfMonth('2028-02')).toBe('2028-02-29');
  });

  it('suma meses cruzando el ano', () => {
    expect(addMonths('2026-01', -1)).toBe('2025-12');
    expect(addMonths('2026-12', 1)).toBe('2027-01');
  });

  /** El 31 de marzo menos un mes es el 28 de febrero, no el 3 de marzo. */
  it('sumar meses a un dia se queda en el ultimo del mes si el suyo no existe', () => {
    expect(addMonthsToDay('2026-03-31', -1)).toBe('2026-02-28');
    expect(addMonthsToDay('2026-09-22', -3)).toBe('2026-06-22');
  });

  it('desmonta un dia en sus partes', () => {
    expect(monthOf('2026-09-22')).toBe('2026-09');
    expect(dayOfMonth('2026-09-22')).toBe(22);
    expect(monthNumber('2026-09')).toBe(9);
    expect(yearOf('2026-09-22')).toBe(2026);
    expect(firstDayOfYear('2026-09-22')).toBe('2026-01-01');
    // El 22 de septiembre de 2026 es martes.
    expect(weekdayOf('2026-09-22')).toBe(2);
  });
});
