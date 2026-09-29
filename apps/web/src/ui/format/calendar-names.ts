import type { AppLanguage } from '../../domain/account/settings';

/**
 * Los nombres de meses y dias del diseno, literales.
 *
 * No salen de `Intl.DateTimeFormat` porque los cortos del CLDR no coinciden: en castellano
 * los devuelve con punto ("sept.") y en ingles cambia el orden a "Sep 22, 2026". Una pantalla
 * hi-fi no puede depender de la version de ICU del navegador, que ademas cambia entre Chrome
 * y Firefox. Son los mismos arrays que `CalendarNames.kt` en Android.
 */
export interface CalendarNames {
  /** Doce, indice 0 = enero. En minuscula: quien los ensena decide si capitaliza. */
  readonly monthsLong: readonly string[];
  /** Doce. Ojo con "sept", de cuatro letras, y con que ninguno lleva punto. */
  readonly monthsShort: readonly string[];
  /** Siete, indice 0 = domingo, que es lo que devuelve `weekdayOf`. */
  readonly weekdaysLong: readonly string[];
}

function words(names: string): readonly string[] {
  return names.split(' ');
}

const SPANISH: CalendarNames = {
  monthsLong: words(
    'enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre',
  ),
  monthsShort: words('ene feb mar abr may jun jul ago sept oct nov dic'),
  weekdaysLong: words('domingo lunes martes miércoles jueves viernes sábado'),
};

const ENGLISH: CalendarNames = {
  monthsLong: words(
    'january february march april may june july august september october november december',
  ),
  monthsShort: words('Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec'),
  weekdaysLong: words('sunday monday tuesday wednesday thursday friday saturday'),
};

export function calendarNamesOf(language: AppLanguage): CalendarNames {
  return language === 'en' ? ENGLISH : SPANISH;
}

/** Sube la primera letra. Los nombres se guardan en minuscula y se capitalizan al pintar. */
export function capitalize(text: string): string {
  return text.slice(0, 1).toUpperCase() + text.slice(1);
}
