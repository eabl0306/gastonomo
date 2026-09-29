import type { AppLanguage } from '../../domain/account/settings';
import {
  addDays,
  dayOfMonth,
  monthNumber,
  monthOf,
  weekdayOf,
  yearOf,
  type Day,
  type Month,
} from '../../domain/calendar/day';

import { calendarNamesOf, capitalize, type CalendarNames } from './calendar-names';

/** "Hoy" y "Ayer". Son textos de la tabla de idioma, no de la tabla de fechas. */
interface DayLabels {
  readonly today: string;
  readonly yesterday: string;
}

/** El guion del rango: en dash U+2013, con un espacio a cada lado. Nunca un guion normal. */
const RANGE_DASH = '\u2013';

/** Las seis formas en las que el diseno ensena una fecha. */
export interface DateFormats {
  /** "22 sept 2026". La fecha de creacion de un prestamo y la de un abono. */
  long: (day: Day) => string;
  /** "22 sept". La fecha dentro del subtitulo de una fila de movimiento. */
  short: (day: Day) => string;
  /** "Septiembre 2026". La etiqueta del selector de mes. */
  monthAndYear: (month: Month) => string;
  /**
   * El mes dentro de una frase: "Balance de septiembre" y "September balance".
   *
   * En castellano va en minuscula porque asi se escribe un mes dentro de una frase; en
   * ingles es nombre propio y lleva mayuscula.
   */
  monthName: (month: Month) => string;
  /** "Martes, 22 sept". La cabecera de Inicio. Lleva coma. */
  weekdayWithDate: (day: Day) => string;
  /** "Hoy", "Ayer" o "Sábado 19 sept". El encabezado de un grupo de dia, **sin** coma. */
  dayGroup: (day: Day, today: Day, labels: DayLabels) => string;
  /** "22 ago – 22 sept 2026". La etiqueta del selector con un rango activo. */
  range: (from: Day, to: Day) => string;
}

function shortOf(names: CalendarNames, day: Day): string {
  return `${String(dayOfMonth(day))} ${names.monthsShort[monthNumber(monthOf(day)) - 1] ?? ''}`;
}

function weekdayNameOf(names: CalendarNames, day: Day): string {
  return names.weekdaysLong[weekdayOf(day)] ?? '';
}

export function dateFormats(language: AppLanguage): DateFormats {
  const names = calendarNamesOf(language);
  const long = (day: Day): string => `${shortOf(names, day)} ${String(yearOf(day))}`;
  return {
    long,
    short: (day) => shortOf(names, day),
    monthAndYear: (month) =>
      `${capitalize(names.monthsLong[monthNumber(month) - 1] ?? '')} ${month.slice(0, 4)}`,
    monthName: (month) => {
      const name = names.monthsLong[monthNumber(month) - 1] ?? '';
      return language === 'en' ? capitalize(name) : name;
    },
    weekdayWithDate: (day) => `${capitalize(weekdayNameOf(names, day))}, ${shortOf(names, day)}`,
    dayGroup: (day, today, labels) => {
      if (day === today) return labels.today;
      if (day === addDays(today, -1)) return labels.yesterday;
      return `${capitalize(weekdayNameOf(names, day))} ${shortOf(names, day)}`;
    },
    range: (from, to) => `${shortOf(names, from)} ${RANGE_DASH} ${long(to)}`,
  };
}
