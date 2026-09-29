import type { AppLanguage, UserSettings } from '../../domain/account/settings';
import { localeOf } from '../../domain/account/settings';
import type { Period } from '../../domain/calendar/period';
import { decimalsOf, type CurrencyCode } from '../../domain/money/currency';
import {
  absDecimal,
  decimalOf,
  compareDecimals,
  signOf,
  type Decimal,
} from '../../domain/money/decimal';
import type { Money } from '../../domain/money/money';

import { FALLBACK_SYMBOLS, symbolOf, type CurrencySymbolTable } from './currency-symbols';
import { dateFormats, type DateFormats } from './date-formats';
import { localizedNumbers, type LocalizedNumbers } from './localized-numbers';

/** El menos tipografico U+2212. El guion normal queda mas corto y desalineado. */
export const MINUS = '\u2212';

/** El mas de un ingreso. */
export const PLUS = '+';

/** Lo que sustituye al importe cuando el ojo esta cerrado: seis U+2022, con simbolo. */
const MASK = '\u2022\u2022\u2022\u2022\u2022\u2022';

/** La mascara corta de las filas de categoria: cuatro U+2022, **sin** simbolo. */
export const MASK_SHORT = '\u2022\u2022\u2022\u2022';

/** El punto medio U+00B7, con un espacio a cada lado, de los subtitulos. */
export const DOT_SEPARATOR = ' \u00b7 ';

const RATE_DECIMALS_SMALL = 6;
const RATE_DECIMALS_MEDIUM = 4;
const RATE_DECIMALS_LARGE = 2;
const ONE = decimalOf('1');
const ONE_HUNDRED = decimalOf('100');

/**
 * Todo el formato que ensena la aplicacion: dinero, tasas y fechas.
 *
 * Es el sitio donde mas facil es que cada pantalla se desvie, asi que no hay nada de esto
 * suelto en ningun componente: se lee del contexto con `useFormats()`.
 */
export interface GastonomoFormats {
  readonly language: AppLanguage;
  readonly displayCurrency: CurrencyCode;
  readonly dates: DateFormats;
  readonly numbers: LocalizedNumbers;
  /** "$ 186.400" o "US$ 140,00". Siempre en valor absoluto: el signo lo ponen los de abajo. */
  amount: (value: Decimal, currency?: CurrencyCode) => string;
  /** Lo mismo para un [Money], que ya lleva su moneda. */
  amountOf: (money: Money) => string;
  /** El importe con su signo: "+" en un ingreso y el menos U+2212 en un gasto. */
  signed: (value: Decimal, isIncome: boolean, currency?: CurrencyCode) => string;
  /** Un neto: "+" si no es negativo. El cero lleva "+". */
  net: (value: Decimal, currency?: CurrencyCode) => string;
  /** Un balance: el menos solo si es negativo, y el positivo sin nada delante. */
  balance: (value: Decimal, currency?: CurrencyCode) => string;
  /** "$ ••••••", lo que ensena la tarjeta de balance con el ojo cerrado. */
  masked: (currency?: CurrencyCode) => string;
  /** Una tasa: 6 decimales por debajo de 1, 4 por debajo de 100 y 2 en el resto. */
  rate: (value: Decimal) => string;
  symbolOf: (currency: CurrencyCode) => string;
  /** La etiqueta del selector: "Septiembre 2026" o "22 ago – 22 sept 2026". */
  periodLabel: (period: Period) => string;
}

function rateDecimals(value: Decimal): number {
  const magnitude = absDecimal(value);
  if (compareDecimals(magnitude, ONE) < 0) return RATE_DECIMALS_SMALL;
  return compareDecimals(magnitude, ONE_HUNDRED) < 0 ? RATE_DECIMALS_MEDIUM : RATE_DECIMALS_LARGE;
}

export function gastonomoFormats(
  settings: UserSettings,
  symbols: CurrencySymbolTable = FALLBACK_SYMBOLS,
): GastonomoFormats {
  const numbers = localizedNumbers(localeOf(settings.language));
  const dates = dateFormats(settings.language);
  const display = settings.displayCurrency;
  const symbol = (currency: CurrencyCode): string => symbolOf(symbols, currency);
  const amount = (value: Decimal, currency: CurrencyCode = display): string =>
    `${symbol(currency)} ${numbers.fixed(absDecimal(value), decimalsOf(currency))}`;
  const signed = (value: Decimal, isIncome: boolean, currency: CurrencyCode = display): string =>
    (isIncome ? PLUS : MINUS) + amount(value, currency);
  return {
    language: settings.language,
    displayCurrency: display,
    dates,
    numbers,
    amount,
    amountOf: (money) => amount(money.amount, money.currency),
    signed,
    net: (value, currency = display) => signed(value, signOf(value) >= 0, currency),
    balance: (value, currency = display) =>
      (signOf(value) < 0 ? MINUS : '') + amount(value, currency),
    masked: (currency = display) => `${symbol(currency)} ${MASK}`,
    rate: (value) => numbers.upTo(value, rateDecimals(value)),
    symbolOf: symbol,
    periodLabel: (period) =>
      period.kind === 'month'
        ? dates.monthAndYear(period.month)
        : dates.range(period.from, period.to),
  };
}
