/**
 * Un dia sin hora ni zona, en `YYYY-MM-DD`.
 *
 * Es el tipo de las fechas de registro de todo el contrato: un gasto del 22 de septiembre
 * es del 22 en Bogota y en Madrid. Se queda en `string` y no en `Date` a proposito, porque
 * un `Date` es un instante y arrastra una zona horaria que aqui no significa nada: en
 * cuanto se cruza medianoche en UTC, el mismo gasto cambia de dia.
 *
 * Las cuentas se hacen convirtiendo a UTC y volviendo, que es el unico calendario sin
 * horario de verano.
 */
export type Day = string;

/** Un mes, en `YYYY-MM`. */
export type Month = string;

const DAY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/u;
const MILLIS_PER_DAY = 86_400_000;

/** True si la cadena es un dia con forma y con sentido: `2026-02-30` no lo es. */
export function isDay(raw: string): boolean {
  const match = DAY_PATTERN.exec(raw);
  if (match === null) return false;
  return toDay(toUtc(raw)) === raw;
}

/** El dia, o null si la cadena no lo es. */
export function parseDay(raw: string): Day | null {
  const text = raw.trim();
  return isDay(text) ? text : null;
}

function toUtc(day: Day): number {
  const match = DAY_PATTERN.exec(day);
  if (match === null) return Number.NaN;
  return Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
}

function toDay(utc: number): Day {
  return new Date(utc).toISOString().slice(0, 10);
}

export function compareDays(a: Day, b: Day): number {
  return a.localeCompare(b);
}

export function isBefore(a: Day, b: Day): boolean {
  return a < b;
}

export function isAfter(a: Day, b: Day): boolean {
  return a > b;
}

export function addDays(day: Day, days: number): Day {
  return toDay(toUtc(day) + days * MILLIS_PER_DAY);
}

/** Dias entre dos fechas, contando el primero y el ultimo. */
export function daysBetween(from: Day, to: Day): number {
  return Math.round((toUtc(to) - toUtc(from)) / MILLIS_PER_DAY) + 1;
}

/** El mes al que pertenece el dia. */
export function monthOf(day: Day): Month {
  return day.slice(0, 7);
}

export function firstDayOfMonth(month: Month): Day {
  return `${month}-01`;
}

/**
 * El ultimo dia del mes. Se calcula pidiendo el dia cero del mes siguiente, que es el
 * ultimo del actual, para no repetir la tabla de dias por mes ni la regla de los bisiestos.
 */
export function lastDayOfMonth(month: Month): Day {
  const year = Number(month.slice(0, 4));
  const index = Number(month.slice(5, 7));
  return toDay(Date.UTC(year, index, 0));
}

export function addMonths(month: Month, months: number): Month {
  const year = Number(month.slice(0, 4));
  const index = Number(month.slice(5, 7)) - 1 + months;
  return toDay(Date.UTC(year, index, 1)).slice(0, 7);
}

/**
 * El mismo dia del mes, [months] meses despues.
 *
 * Si ese dia no existe en el mes de destino se queda en el ultimo, que es lo que hace
 * `java.time` y lo que espera cualquiera: el 31 de marzo menos un mes es el 28 de febrero,
 * no el 3 de marzo.
 */
export function addMonthsToDay(day: Day, months: number): Day {
  const target = addMonths(monthOf(day), months);
  const last = lastDayOfMonth(target);
  const candidate = `${target}-${day.slice(8, 10)}`;
  return candidate > last ? last : candidate;
}

export function firstDayOfYear(day: Day): Day {
  return `${day.slice(0, 4)}-01-01`;
}

/** El dia de la semana, de 0 (domingo) a 6 (sabado). Es el indice de los nombres. */
export function weekdayOf(day: Day): number {
  return new Date(toUtc(day)).getUTCDay();
}

/** El dia del mes, de 1 a 31. */
export function dayOfMonth(day: Day): number {
  return Number(day.slice(8, 10));
}

/** El mes, de 1 a 12. */
export function monthNumber(month: Month): number {
  return Number(month.slice(5, 7));
}

export function yearOf(day: Day): number {
  return Number(day.slice(0, 4));
}
